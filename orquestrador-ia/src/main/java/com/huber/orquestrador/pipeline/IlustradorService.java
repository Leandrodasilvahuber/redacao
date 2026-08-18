package com.huber.orquestrador.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.configuracao.EstiloIlustracao;
import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.gemini.LimiteGeminiAtingidoException;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.mistral.MistralClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IlustradorService {

    private static final Logger log = LoggerFactory.getLogger(IlustradorService.class);

    private static final String PROMPT_SISTEMA = """
            Você prepara um post de LinkedIn sobre tecnologia para publicação, em duas partes:

            1. "texto": o post adequado ao formato do feed do LinkedIn
               - SINTETIZE: corte frases redundantes e explicações repetidas, vá direto ao ponto
               - Alvo de 400 a 700 caracteres (bem mais curto que o texto original), sem perder a ideia central
               - Não invente informação nova, só condense o que já existe
               - Pode adicionar poucos emojis relevantes (sem exagero) para guiar a leitura
               - Parágrafos curtos e bem espaçados, sem markdown, sem asteriscos
               - Preserve as hashtags no final

            2. "svg": a PRIMEIRA de várias ilustrações de capa para o post — um DESENHO/ILUSTRAÇÃO vetorial
               de verdade (nunca uma foto, nunca uma imagem de banco de imagens, nunca um banner) — use %s,
               em SVG completo e autocontido
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

            Responda somente com o JSON pedido, sem comentários adicionais.
            """;

    private static final String PROMPT_SVG_ADICIONAL = """
            Você é um ilustrador vetorial. Já existe(m) ilustração(ões) de capa para este post de LinkedIn
            sobre uma notícia de tecnologia (mostradas abaixo). Crie mais UM desenho, diferente e complementar,
            sobre o mesmo tema — outro ângulo, cena ou elemento (não repita a composição das anteriores).
            - DESENHO/ILUSTRAÇÃO vetorial de verdade (nunca uma foto, nunca um banner abstrato), use %s
            - SVG completo e autocontido, viewBox="0 0 1200 627"
            - Cena figurativa com formas reconhecíveis, vários elementos, sombras suaves, paleta vibrante
              e consistente com o tema e com as ilustrações anteriores
            - Use apenas formas geométricas/vetoriais, gradientes e texto (sem <image>, sem links externos, sem <script>)
            Responda apenas com o código SVG completo, sem comentários, sem markdown, sem crases.
            """;

    private static final int TOTAL_ILUSTRACOES = 3;
    private static final int REFINAMENTOS_MISTRAL = 3;

    private static final String PROMPT_REFINAMENTO_SVG_MISTRAL = """
            Você é um ilustrador vetorial revisando o próprio trabalho. Abaixo está uma ilustração SVG
            que você mesmo criou como capa para um post de LinkedIn sobre a notícia indicada.
            Revise criticamente e produza uma VERSÃO MELHORADA do mesmo SVG: mais elementos, cores mais
            harmoniosas e vibrantes, composição mais clara e profissional — mantendo o mesmo tema e
            estilo, use %s
            - SVG completo e autocontido, viewBox="0 0 1200 627"
            - Use apenas formas geométricas/vetoriais, gradientes e texto (sem <image>, sem links externos, sem <script>)
            Responda apenas com o código SVG completo revisado, sem comentários, sem markdown, sem crases.
            """;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "texto", Map.of("type", "STRING"),
                    "svg", Map.of("type", "STRING")
            ),
            "required", List.of("texto", "svg")
    );

    private static final int PROVEDOR_GEMINI = 0;
    private static final int PROVEDOR_MISTRAL = 1;

    private final NoticiaRepository noticiaRepository;
    private final GeminiClient geminiClient;
    private final MistralClient mistralClient;
    private final ObjectMapper objectMapper;
    private final ConfiguracaoService configuracaoService;

    public IlustradorService(NoticiaRepository noticiaRepository, GeminiClient geminiClient,
                              MistralClient mistralClient, ObjectMapper objectMapper,
                              ConfiguracaoService configuracaoService) {
        this.noticiaRepository = noticiaRepository;
        this.geminiClient = geminiClient;
        this.mistralClient = mistralClient;
        this.objectMapper = objectMapper;
        this.configuracaoService = configuracaoService;
    }

    private String montarPromptSistema() {
        EstiloIlustracao estilo = configuracaoService.getEstiloIlustracao();
        return PROMPT_SISTEMA.formatted(estilo.getDescricaoPrompt());
    }

    private String montarPromptSvgAdicional() {
        EstiloIlustracao estilo = configuracaoService.getEstiloIlustracao();
        return PROMPT_SVG_ADICIONAL.formatted(estilo.getDescricaoPrompt());
    }

    private String montarPromptRefinamentoMistral() {
        EstiloIlustracao estilo = configuracaoService.getEstiloIlustracao();
        return PROMPT_REFINAMENTO_SVG_MISTRAL.formatted(estilo.getDescricaoPrompt());
    }

    /**
     * Usa o Gemini primeiro; se a cota diária estourar, passa a usar o Mistral pelo resto da execução.
     */
    private String chatComFallback(int[] provedorAtual, String systemPrompt, String userPrompt, boolean pedirJson) {
        if (provedorAtual[0] == PROVEDOR_GEMINI) {
            try {
                return pedirJson
                        ? geminiClient.chat(systemPrompt, userPrompt, SCHEMA)
                        : geminiClient.chat(systemPrompt, userPrompt);
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, trocando para Mistral pelo resto da execução: {}", e.getMessage());
                provedorAtual[0] = PROVEDOR_MISTRAL;
            }
        }
        return mistralClient.chat(systemPrompt, userPrompt, pedirJson);
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
        int[] provedorAtual = {PROVEDOR_GEMINI};
        String promptSistema = montarPromptSistema();
        String promptSvgAdicional = montarPromptSvgAdicional();
        String promptRefinamentoMistral = montarPromptRefinamentoMistral();

        for (Noticia noticia : revisadas) {
            String pergunta = "Título: " + noticia.getTitulo() + "\nTexto revisado:\n" + noticia.getTextoRevisado();

            Resultado resultado;
            try {
                String json = chatComFallback(provedorAtual, promptSistema, pergunta, true);
                resultado = objectMapper.readValue(json, Resultado.class);
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Parando ilustração (limite dos provedores atingido): {}", e.getMessage());
                break;
            } catch (Exception e) {
                log.warn("Falha ao interpretar resposta para a notícia {}: {}", noticia.getId(), e.getMessage());
                continue;
            }

            List<String> svgs = new ArrayList<>();
            if (provedorAtual[0] == PROVEDOR_MISTRAL) {
                // Mistral gera pior variedade em ilustrações distintas: em vez de 3 desenhos diferentes,
                // gera 1 e refina o mesmo desenho 3 vezes.
                String svgAtual = resultado.svg();
                for (int i = 0; i < REFINAMENTOS_MISTRAL; i++) {
                    try {
                        String pedido = "Título: " + noticia.getTitulo() + "\nSVG atual:\n" + svgAtual;
                        svgAtual = chatComFallback(provedorAtual, promptRefinamentoMistral, pedido, false);
                    } catch (LimiteMistralAtingidoException e) {
                        log.warn("Parando refinamento da ilustração (limite do Mistral atingido), ficando com a versão atual: {}",
                                e.getMessage());
                        break;
                    } catch (Exception e) {
                        log.warn("Falha ao refinar ilustração da notícia {} (mantendo a versão atual): {}",
                                noticia.getId(), e.getMessage());
                        break;
                    }
                }
                svgs.add(svgAtual);
            } else {
                svgs.add(resultado.svg());
                for (int i = 1; i < TOTAL_ILUSTRACOES; i++) {
                    try {
                        String pedido = "Título: " + noticia.getTitulo()
                                + "\nIlustrações já existentes (SVG):\n" + String.join("\n---\n", svgs);
                        svgs.add(chatComFallback(provedorAtual, promptSvgAdicional, pedido, false));
                    } catch (LimiteMistralAtingidoException e) {
                        log.warn("Parando geração de mais ilustrações (fica com as {} já geradas): {}",
                                svgs.size(), e.getMessage());
                        break;
                    } catch (Exception e) {
                        log.warn("Falha ao gerar ilustração extra para a notícia {} (mantendo as já geradas): {}",
                                noticia.getId(), e.getMessage());
                        break;
                    }
                }
            }

            noticia.setTextoIlustrado(resultado.texto());
            noticia.setSvgIlustracao(objectMapper.writeValueAsString(svgs));
            noticia.mudarEstado(EstadoNoticia.ILUSTRADA);
            noticiaRepository.save(noticia);
            ilustradas++;
        }
        return ilustradas;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Resultado(String texto, String svg) {
    }
}
