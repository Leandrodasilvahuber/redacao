package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.configuracao.EstiloIlustracao;
import com.huber.orquestrador.configuracao.ProvedorIlustracao;
import com.huber.orquestrador.flux.FluxClient;
import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.gemini.LimiteGeminiAtingidoException;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class IlustradorService {

    private static final Logger log = LoggerFactory.getLogger(IlustradorService.class);

    private static final String PROMPT_TEXTO = """
            Você prepara o texto de um post de LinkedIn sobre tecnologia para publicação, a partir do
            texto revisado abaixo.
            - SINTETIZE: corte frases redundantes e explicações repetidas, vá direto ao ponto
            - Alvo de 400 a 700 caracteres (bem mais curto que o texto original), sem perder a ideia central
            - Não invente informação nova, só condense o que já existe
            - Pode adicionar poucos emojis relevantes (sem exagero) para guiar a leitura
            - Parágrafos curtos e bem espaçados, sem markdown, sem asteriscos
            - Preserve as hashtags no final
            Responda apenas com o texto do post, sem comentários adicionais.
            """;

    private static final String PROMPT_SVG_GEMINI = """
            Você é um ilustrador vetorial. Desenhe UMA ilustração de capa para um post de LinkedIn sobre a
            notícia de tecnologia indicada — um DESENHO/ILUSTRAÇÃO vetorial de verdade (nunca uma foto, nunca
            uma imagem de banco de imagens, nunca um banner) — use %s, em SVG completo e autocontido
            - viewBox="0 0 1200 627"
            - Desenhe uma cena figurativa e reconhecível ligada ao tema da notícia: objetos, personagens
              estilizados (bonecos simples, mãos, rostos), cenário — algo que conte a história em uma imagem
            - Componha com vários elementos (objeto principal + elementos de apoio, sombras suaves,
              formas de fundo) para dar profundidade, como um desenho editorial de verdade
            - Use uma paleta de cores vibrante e variada (não se limite a tons escuros de azul/cinza) —
              escolha cores que combinem com o tema da notícia
            - PROIBIDO: banner abstrato só com gradiente de fundo e texto grande no meio
            - Pode ter um pequeno título ou rótulo textual de apoio, mas o foco é o desenho
            - Use apenas formas geométricas/vetoriais, gradientes e texto (sem <image>, sem links externos, sem <script>)
            - Composição limpa, colorida e profissional
            Responda apenas com o código SVG completo, sem comentários, sem markdown, sem crases.
            """;

    private static final int LARGURA_IMAGEM = 1200;
    private static final int ALTURA_IMAGEM = 627;

    private final NoticiaRepository noticiaRepository;
    private final GeminiClient geminiClient;
    private final FluxClient fluxClient;
    private final ObjectMapper objectMapper;
    private final ConfiguracaoService configuracaoService;

    public IlustradorService(NoticiaRepository noticiaRepository, GeminiClient geminiClient,
                              FluxClient fluxClient, ObjectMapper objectMapper,
                              ConfiguracaoService configuracaoService) {
        this.noticiaRepository = noticiaRepository;
        this.geminiClient = geminiClient;
        this.fluxClient = fluxClient;
        this.objectMapper = objectMapper;
        this.configuracaoService = configuracaoService;
    }

    private String montarPromptSvgGemini() {
        EstiloIlustracao estilo = configuracaoService.getEstiloIlustracao();
        return PROMPT_SVG_GEMINI.formatted(estilo.getDescricaoPrompt());
    }

    public int ilustrar() {
        return ilustrar(null);
    }

    public int ilustrar(Long id) {
        List<Noticia> revisadas = id != null
                ? noticiaRepository.findById(id)
                        .filter(n -> n.getEstado() == EstadoNoticia.REVISADA)
                        .map(List::of)
                        .orElseGet(List::of)
                : noticiaRepository.findByEstado(EstadoNoticia.REVISADA);

        int ilustradas = 0;
        boolean[] geminiEsgotado = {false};
        ProvedorIlustracao provedor = configuracaoService.getProvedorIlustracao();
        String promptSvgGemini = montarPromptSvgGemini();

        for (Noticia noticia : revisadas) {
            String textoIlustrado = gerarTextoIlustrado(noticia, geminiEsgotado);
            String ilustracao = gerarIlustracao(noticia, provedor, promptSvgGemini, geminiEsgotado);

            if (ilustracao == null) {
                log.warn("Sem ilustração disponível para a notícia {} (Gemini e Flux falharam)", noticia.getId());
                continue;
            }

            noticia.setTextoIlustrado(textoIlustrado);
            noticia.setSvgIlustracao(objectMapper.writeValueAsString(List.of(ilustracao)));
            noticia.mudarEstado(EstadoNoticia.ILUSTRADA);
            noticiaRepository.save(noticia);
            ilustradas++;
        }
        return ilustradas;
    }

    /**
     * O texto do post sempre passa pelo Gemini (única IA de texto usada nesta etapa). Se a cota diária
     * estourar ou a chamada falhar, usa o texto já revisado sem condensar.
     */
    private String gerarTextoIlustrado(Noticia noticia, boolean[] geminiEsgotado) {
        if (geminiEsgotado[0]) {
            return noticia.getTextoRevisado();
        }
        try {
            String pergunta = "Título: " + noticia.getTitulo() + "\nTexto revisado:\n" + noticia.getTextoRevisado();
            return geminiClient.chat(PROMPT_TEXTO, pergunta);
        } catch (LimiteGeminiAtingidoException e) {
            log.warn("Limite do Gemini atingido, usando o texto revisado sem condensar: {}", e.getMessage());
            geminiEsgotado[0] = true;
            return noticia.getTextoRevisado();
        } catch (Exception e) {
            log.warn("Falha ao gerar texto do post para a notícia {} (usando texto revisado sem condensar): {}",
                    noticia.getId(), e.getMessage());
            return noticia.getTextoRevisado();
        }
    }

    /**
     * Gemini desenha o SVG por padrão; se a cota diária estourar ou a chamada falhar, cai para o Flux
     * Schnell (Pollinations, sem chave e praticamente ilimitado) pelo resto da execução. Se o provedor
     * configurado já for o Flux, vai direto para ele.
     */
    private String gerarIlustracao(Noticia noticia, ProvedorIlustracao provedor, String promptSvgGemini,
                                    boolean[] geminiEsgotado) {
        if (provedor == ProvedorIlustracao.GEMINI && !geminiEsgotado[0]) {
            try {
                String pedido = "Título: " + noticia.getTitulo() + "\nTexto revisado:\n" + noticia.getTextoRevisado();
                return geminiClient.chat(promptSvgGemini, pedido);
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, usando Flux (Pollinations) pelo resto da execução: {}",
                        e.getMessage());
                geminiEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao desenhar ilustração no Gemini para a notícia {}, tentando Flux (Pollinations): {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return gerarImagemFlux(noticia);
    }

    private String gerarImagemFlux(Noticia noticia) {
        try {
            String prompt = montarPromptFlux(noticia);
            long seed = noticia.getId() != null ? noticia.getId() : System.currentTimeMillis();
            return fluxClient.gerarImagemBase64(prompt, LARGURA_IMAGEM, ALTURA_IMAGEM, seed);
        } catch (Exception e) {
            log.warn("Falha ao gerar ilustração no Flux (Pollinations) para a notícia {}: {}",
                    noticia.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Prompts longos com o corpo da notícia (em português, com tom editorial abstrato) fazem o Flux cair
     * num retrato genérico sem relação com o tema. Focar só no título, com objetos concretos do tema e uma
     * negativa explícita contra retratos/pessoas realistas, dá resultados muito mais aderentes ao assunto.
     */
    private String montarPromptFlux(Noticia noticia) {
        EstiloIlustracao estilo = configuracaoService.getEstiloIlustracao();
        return "Flat vector editorial illustration, tech blog cover style, " + estilo.getDescricaoPrompt() + ". "
                + "Central scene built from concrete technology icons, devices and symbols related to: "
                + noticia.getTitulo()
                + " -- arranged as a clear editorial composition. "
                + "No human faces, no portraits, no photorealistic people. "
                + "Vibrant varied color palette, clean flat design, wide banner, "
                + "no readable text, no watermark, no logo.";
    }
}
