package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.gemini.LimiteGeminiAtingidoException;
import com.huber.orquestrador.iconify.IconeSvgUtil;
import com.huber.orquestrador.iconify.IconifyClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta a capa dos posts a partir de um ícone real (buscado no Iconify, sem IA de desenho) e de um
 * dos 4 layouts fixos em {@link LayoutIlustracao}. A IA (Gemini) só decide dois pedaços pequenos e
 * seguros: qual termo buscar no Iconify e qual "acabamento" (layout, cores, fonte) usar, para os
 * posts variarem e ficarem criativos sem depender de um desenho livre e instável.
 */
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

    private static final String PROMPT_ACABAMENTO = """
            Você decide o acabamento visual da capa de um post de LinkedIn sobre a notícia de tecnologia
            indicada. A composição em si (posição do ícone, do texto) já é um template fixo — você só
            escolhe:
            - "termoIcone": UMA palavra ou expressão curta EM INGLÊS, simples e concreta, para buscar um
              ícone relacionado ao tema no Iconify (ex.: "robot", "cloud", "database", "lock", "chip",
              "rocket"). Evite termos abstratos.
            - "layout": um número de 1 a 4, varie entre os posts.
            - "corFundoInicio" e "corFundoFim": cores hexadecimais (#rrggbb) para o gradiente/painel de
              fundo. Mantenha SEMPRE um fundo escuro e sofisticado (preto ou tons quase pretos, como
              #000000, #0a0a0a, #111111, #0d1117, podendo puxar levemente para a cor do tema) — nunca um
              fundo claro ou muito colorido.
            - "corDestaque": cor hexadecimal vibrante de destaque (forma geométrica atrás do ícone e cor
              do próprio ícone), escolhida para combinar com o tema da notícia e contrastar bem com o
              fundo escuro.
            - "corTexto": cor hexadecimal do título — sempre bem clara (branco ou quase branco) para ler
              bem sobre o fundo escuro.
            - "fonte": uma destas quatro opções: "SANS_GEOMETRICA", "SERIF_EDITORIAL", "MONO_TECH",
              "SANS_ARREDONDADA". O título é sempre em negrito (bold).
            Varie a fonte e a cor de destaque de post para post, mas o fundo continua sempre escuro.
            """;

    private static final Map<String, Object> SCHEMA_ACABAMENTO = Map.of(
            "type", "OBJECT",
            "properties", new LinkedHashMap<>(Map.of(
                    "termoIcone", Map.of("type", "STRING"),
                    "layout", Map.of("type", "STRING", "enum", List.of("1", "2", "3", "4")),
                    "corFundoInicio", Map.of("type", "STRING"),
                    "corFundoFim", Map.of("type", "STRING"),
                    "corDestaque", Map.of("type", "STRING"),
                    "corTexto", Map.of("type", "STRING"),
                    "fonte", Map.of("type", "STRING", "enum",
                            List.of("SANS_GEOMETRICA", "SERIF_EDITORIAL", "MONO_TECH", "SANS_ARREDONDADA"))
            )),
            "required", List.of("termoIcone", "layout", "corFundoInicio", "corFundoFim", "corDestaque",
                    "corTexto", "fonte")
    );

    private static final Map<String, String> FONTES = Map.of(
            "SANS_GEOMETRICA", "'Poppins','Segoe UI',sans-serif",
            "SERIF_EDITORIAL", "'Georgia','Playfair Display',serif",
            "MONO_TECH", "'JetBrains Mono','Courier New',monospace",
            "SANS_ARREDONDADA", "'Verdana','Trebuchet MS',sans-serif"
    );

    private static final Pattern COR_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static final String ICONE_FALLBACK_CPU =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"currentColor\" d=\"M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"
                    + "M9 9h6v6H9z M9 1v2 M15 1v2 M9 21v2 M15 21v2 M1 9h2 M1 15h2 M21 9h2 M21 15h2\"/></svg>";

    private final NoticiaRepository noticiaRepository;
    private final GeminiClient geminiClient;
    private final IconifyClient iconifyClient;
    private final ObjectMapper objectMapper;

    public IlustradorService(NoticiaRepository noticiaRepository, GeminiClient geminiClient,
                              IconifyClient iconifyClient, ObjectMapper objectMapper) {
        this.noticiaRepository = noticiaRepository;
        this.geminiClient = geminiClient;
        this.iconifyClient = iconifyClient;
        this.objectMapper = objectMapper;
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

        for (Noticia noticia : revisadas) {
            String textoIlustrado = gerarTextoIlustrado(noticia, geminiEsgotado);
            String ilustracao = gerarIlustracao(noticia, geminiEsgotado);

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
     * Monta a capa: pede o acabamento (termo do ícone + layout + cores + fonte) ao Gemini, busca o
     * ícone no Iconify e renderiza um dos 4 templates fixos. Cada etapa tem um fallback seguro, então
     * a ilustração praticamente nunca falha por completo.
     */
    private String gerarIlustracao(Noticia noticia, boolean[] geminiEsgotado) {
        Acabamento acabamento = gerarAcabamento(noticia, geminiEsgotado);
        String iconeSvg = buscarIconeComFallback(acabamento.termoIcone());
        return renderizar(acabamento, iconeSvg, noticia.getTitulo());
    }

    private Acabamento gerarAcabamento(Noticia noticia, boolean[] geminiEsgotado) {
        if (!geminiEsgotado[0]) {
            try {
                String pedido = "Título: " + noticia.getTitulo();
                String json = geminiClient.chat(PROMPT_ACABAMENTO, pedido, SCHEMA_ACABAMENTO);
                return Acabamento.doJson(objectMapper, json);
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, usando acabamento padrão: {}", e.getMessage());
                geminiEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao decidir o acabamento da capa para a notícia {}, usando padrão: {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return Acabamento.padrao(noticia);
    }

    private String buscarIconeComFallback(String termoIcone) {
        try {
            return iconifyClient.buscarIconeSvg(termoIcone);
        } catch (Exception e) {
            log.warn("Falha ao buscar ícone \"{}\" no Iconify, usando ícone genérico: {}", termoIcone, e.getMessage());
            return ICONE_FALLBACK_CPU;
        }
    }

    private String renderizar(Acabamento acabamento, String iconeSvg, String titulo) {
        LayoutIlustracao layout = LayoutIlustracao.doIndice(acabamento.layout());
        String icone = IconeSvgUtil.posicionar(iconeSvg, layout.iconeCx, layout.iconeCy, layout.iconeTamanho,
                acabamento.corDestaque());

        double tituloX = layout == LayoutIlustracao.LATERAL ? 500
                : layout == LayoutIlustracao.DIAGONAL ? 90
                : 600;
        int maxCaracteresPorLinha = layout == LayoutIlustracao.LATERAL || layout == LayoutIlustracao.DIAGONAL ? 22 : 30;
        String tituloTspans = TituloSvgUtil.tspans(titulo, tituloX, 54, maxCaracteresPorLinha, 3);

        String svg = layout.montar(icone, tituloTspans);
        return svg
                .replace("{{BG1}}", acabamento.corFundoInicio())
                .replace("{{BG2}}", acabamento.corFundoFim())
                .replace("{{ACCENT}}", acabamento.corDestaque())
                .replace("{{TEXT}}", acabamento.corTexto())
                .replace("{{FONT}}", FONTES.getOrDefault(acabamento.fonte(), FONTES.get("SANS_GEOMETRICA")));
    }

    private record Acabamento(String termoIcone, int layout, String corFundoInicio, String corFundoFim,
                               String corDestaque, String corTexto, String fonte) {

        static Acabamento doJson(ObjectMapper mapper, String json) {
            Map<?, ?> dados = mapper.readValue(json, Map.class);
            String termoIcone = textoSeguro(dados.get("termoIcone"), "technology");
            int layout = layoutSeguro(dados.get("layout"));
            String bg1 = hexSeguro(dados.get("corFundoInicio"), "#000000");
            String bg2 = hexSeguro(dados.get("corFundoFim"), "#111111");
            String destaque = hexSeguro(dados.get("corDestaque"), "#38bdf8");
            String texto = hexSeguro(dados.get("corTexto"), "#e2e8f0");
            String fonte = FONTES.containsKey(dados.get("fonte")) ? (String) dados.get("fonte") : "SANS_GEOMETRICA";
            return new Acabamento(termoIcone, layout, bg1, bg2, destaque, texto, fonte);
        }

        static Acabamento padrao(Noticia noticia) {
            long semente = noticia.getId() != null ? noticia.getId() : System.currentTimeMillis();
            int layout = (int) (Math.abs(semente) % 4) + 1;
            return new Acabamento("technology", layout, "#000000", "#111111", "#38bdf8", "#f8fafc",
                    "SANS_GEOMETRICA");
        }

        private static String textoSeguro(Object valor, String padrao) {
            return valor instanceof String s && !s.isBlank() ? s.trim() : padrao;
        }

        private static int layoutSeguro(Object valor) {
            try {
                return Integer.parseInt(String.valueOf(valor).trim());
            } catch (Exception e) {
                return 1;
            }
        }

        private static String hexSeguro(Object valor, String padrao) {
            String texto = valor instanceof String s ? s.trim() : "";
            Matcher matcher = COR_HEX.matcher(texto);
            return matcher.matches() ? texto : padrao;
        }
    }
}
