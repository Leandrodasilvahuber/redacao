package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.gemini.LimiteGeminiAtingidoException;
import com.huber.orquestrador.iconify.IconeSvgUtil;
import com.huber.orquestrador.iconify.IconifyClient;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.mistral.MistralClient;
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
 * Monta a capa dos posts reproduzindo a identidade visual fixa do blog (fundo escuro quadriculado +
 * ícone grande com brilho neon, sem título desenhado — o blog já mostra o título como texto). A IA
 * (Gemini) só decide o termo de busca do ícone no Iconify e a cor de destaque entre as quatro cores
 * da paleta do blog.
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
            Você escolhe o ícone da capa de um post sobre a notícia de tecnologia indicada. A capa segue
            sempre a identidade visual fixa do blog (fundo escuro quadriculado com o ícone grande e
            brilho neon no centro — sem título desenhado na imagem, o título já aparece como texto do
            post) — você só escolhe:
            - "termoIcone": UMA palavra ou expressão curta EM INGLÊS, simples e concreta, para buscar um
              ícone relacionado ao tema no Iconify (ex.: "robot", "cloud", "database", "lock", "chip",
              "rocket"). Evite termos abstratos.
            - "corDestaque": cor dos pontinhos de destaque ao lado do ícone (o ícone em si é sempre
              ciano claro, igual ao line-art fixo do blog). Escolha UMA destas quatro cores da
              identidade visual do blog, a que combinar melhor com o tema da notícia: "#00F0FF" (ciano,
              uso geral/tech), "#8CF7FF" (ciano claro, dados e redes), "#FF2E9A" (magenta, segurança e
              alertas), "#9D4EFF" (roxo, IA e produto). Varie entre os posts.
            """;

    private static final String PROMPT_ICONE = """
            Você escolhe só o ícone da capa de um post sobre a notícia de tecnologia indicada (a cor de
            destaque já está definida e não muda). Responda com:
            - "termoIcone": UMA palavra ou expressão curta EM INGLÊS, simples e concreta, para buscar um
              ícone relacionado ao tema no Iconify (ex.: "robot", "cloud", "database", "lock", "chip",
              "rocket"). Evite termos abstratos. Se o pedido indicar termos já usados antes, escolha
              OBRIGATORIAMENTE um termo diferente de todos eles, mesmo que ainda ligado ao tema. Se o
              pedido incluir uma descrição adicional do usuário, priorize um termo que reflita essa
              descrição, mantendo a coerência com o título.
            """;

    private static final Map<String, Object> SCHEMA_ICONE = Map.of(
            "type", "OBJECT",
            "properties", Map.of("termoIcone", Map.of("type", "STRING")),
            "required", List.of("termoIcone")
    );

    private static final String PROMPT_ICONE_MISTRAL = PROMPT_ICONE + """

            Responda em JSON válido e apenas o JSON (sem texto fora dele, sem markdown), com exatamente
            esta chave: "termoIcone" (string).
            """;

    /**
     * Termos de reserva pra garantir um ícone diferente do anterior mesmo se as IAs insistirem em
     * repetir o termo já usado (a busca no Iconify é determinística por termo).
     */
    private static final List<String> TERMOS_ICONE_RESERVA =
            List.of("chip", "circuit", "spark", "beacon", "signal", "orbit", "pulse", "network");

    /** Quantidade de termos de ícone recentes guardados por notícia, pra evitar repetir ao regerar. */
    private static final int MAX_HISTORICO_TERMOS_ICONE = 5;

    private static final List<String> CORES_BLOG = List.of("#00F0FF", "#8CF7FF", "#FF2E9A", "#9D4EFF");

    private static final Map<String, Object> SCHEMA_ACABAMENTO = Map.of(
            "type", "OBJECT",
            "properties", new LinkedHashMap<>(Map.of(
                    "termoIcone", Map.of("type", "STRING"),
                    "corDestaque", Map.of("type", "STRING", "enum", CORES_BLOG)
            )),
            "required", List.of("termoIcone", "corDestaque")
    );

    private static final String PROMPT_ACABAMENTO_MISTRAL = PROMPT_ACABAMENTO + """

            Responda em JSON válido e apenas o JSON (sem texto fora dele, sem markdown), com exatamente
            estas chaves: "termoIcone" (string) e "corDestaque" (uma destas quatro strings, exatamente
            como escrito: "#00F0FF", "#8CF7FF", "#FF2E9A", "#9D4EFF").
            """;

    /** Fundo (var(--card-2)) da identidade visual do blog. */
    private static final String COR_FUNDO_BLOG = "#171725";

    /** Biblioteca de ícones do Iconify usada na capa — traço fino, igual ao line-art fixo do blog. */
    private static final String BIBLIOTECA_ICONES = "tabler";

    private static final String ICONE_FALLBACK_CPU =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"currentColor\" d=\"M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"
                    + "M9 9h6v6H9z M9 1v2 M15 1v2 M9 21v2 M15 21v2 M1 9h2 M1 15h2 M21 9h2 M21 15h2\"/></svg>";

    private final NoticiaRepository noticiaRepository;
    private final GeminiClient geminiClient;
    private final MistralClient mistralClient;
    private final IconifyClient iconifyClient;
    private final ObjectMapper objectMapper;

    public IlustradorService(NoticiaRepository noticiaRepository, GeminiClient geminiClient,
                              MistralClient mistralClient, IconifyClient iconifyClient,
                              ObjectMapper objectMapper) {
        this.noticiaRepository = noticiaRepository;
        this.geminiClient = geminiClient;
        this.mistralClient = mistralClient;
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
        boolean[] mistralEsgotado = {false};

        for (Noticia noticia : revisadas) {
            String textoIlustrado = gerarTextoIlustrado(noticia, geminiEsgotado, mistralEsgotado);
            String ilustracao = gerarIlustracao(noticia, geminiEsgotado, mistralEsgotado);

            noticia.setTextoIlustrado(textoIlustrado);
            noticia.setSvgIlustracao(objectMapper.writeValueAsString(List.of(ilustracao)));
            noticia.mudarEstado(EstadoNoticia.ILUSTRADA);
            noticiaRepository.save(noticia);
            ilustradas++;
        }
        return ilustradas;
    }

    /**
     * Gera uma nova capa pra uma notícia já ilustrada (ou publicada), no padrão visual atual, e
     * salva no lugar da anterior. Usado pra atualizar posts antigos sem repetir o resto do
     * pipeline (texto, seleção, etc.).
     */
    public String regerarIlustracao(Noticia noticia) {
        String ilustracao = gerarIlustracao(noticia, new boolean[]{false}, new boolean[]{false});
        noticia.setSvgIlustracao(objectMapper.writeValueAsString(List.of(ilustracao)));
        noticiaRepository.save(noticia);
        return ilustracao;
    }

    /**
     * Gera de novo só o ícone da capa, mantendo a cor de destaque atual (lida da capa já persistida),
     * e salva no lugar da anterior. {@code descricaoUsuario} é opcional e, quando informada, guia a
     * IA na escolha do termo do ícone.
     */
    public String regerarIcone(Noticia noticia, String descricaoUsuario) {
        String corAtual = corAtualDaIlustracao(noticia);
        List<String> termosUsados = lerHistoricoTermos(noticia.getTermosIconeUsados());
        String termoIcone = gerarTermoIcone(noticia, termosUsados, descricaoUsuario,
                new boolean[]{false}, new boolean[]{false});
        String termoEscolhido = termoIcone;
        if (termosUsados.stream().anyMatch(t -> t.equalsIgnoreCase(termoEscolhido))) {
            log.warn("IA repetiu um termo de ícone já usado recentemente ({}) na notícia {}, forçando um termo diferente",
                    termosUsados, noticia.getId());
            termoIcone = termoDiferente(termosUsados);
        }
        String iconeSvg = buscarIconeComFallback(termoIcone);
        String ilustracao = renderizar(new Acabamento(termoIcone, corAtual), iconeSvg);
        noticia.setSvgIlustracao(objectMapper.writeValueAsString(List.of(ilustracao)));
        noticia.setTermosIconeUsados(objectMapper.writeValueAsString(comHistoricoAtualizado(termosUsados, termoIcone)));
        noticiaRepository.save(noticia);
        return ilustracao;
    }

    /** Lê o histórico de termos de ícone já usados, ou lista vazia se não houver (ainda) histórico salvo. */
    private List<String> lerHistoricoTermos(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            log.warn("Falha ao ler o histórico de termos de ícone, tratando como vazio: {}", e.getMessage());
            return List.of();
        }
    }

    /** Acrescenta o novo termo ao histórico e mantém só os últimos {@link #MAX_HISTORICO_TERMOS_ICONE}. */
    private static List<String> comHistoricoAtualizado(List<String> atual, String novoTermo) {
        List<String> atualizado = new java.util.ArrayList<>(atual);
        atualizado.removeIf(t -> t.equalsIgnoreCase(novoTermo));
        atualizado.add(novoTermo);
        int inicio = Math.max(0, atualizado.size() - MAX_HISTORICO_TERMOS_ICONE);
        return atualizado.subList(inicio, atualizado.size());
    }

    /** Termo de reserva que evita todos os termos informados, pra garantir variação mesmo se a IA insistir em repetir. */
    private static String termoDiferente(List<String> termosEvitar) {
        return TERMOS_ICONE_RESERVA.stream()
                .filter(termo -> termosEvitar.stream().noneMatch(t -> t.equalsIgnoreCase(termo)))
                .findFirst()
                .orElse(TERMOS_ICONE_RESERVA.get(0));
    }

    /** Lê a cor de destaque da capa já persistida, ou sorteia uma cor padrão se não houver capa ainda. */
    private String corAtualDaIlustracao(Noticia noticia) {
        String svgAtual = noticia.getSvgIlustracao();
        if (svgAtual != null && !svgAtual.isBlank()) {
            try {
                String[] ilustracoes = objectMapper.readValue(svgAtual, String[].class);
                if (ilustracoes.length > 0) {
                    Matcher m = Pattern.compile("r=\"9\" fill=\"(#[0-9A-Fa-f]{6})\"").matcher(ilustracoes[0]);
                    if (m.find()) {
                        String cor = m.group(1).toUpperCase();
                        if (CORES_BLOG.contains(cor)) {
                            return cor;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Falha ao ler a cor de destaque atual da notícia {}, usando cor padrão: {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return Acabamento.padrao(noticia).corDestaque();
    }

    /**
     * Prefere o Gemini para escolher o novo termo do ícone; quando a cota diária dele estourar, passa
     * a usar o Mistral pelo resto da execução. Só cai no termo padrão se as duas IAs estiverem
     * indisponíveis.
     */
    private String gerarTermoIcone(Noticia noticia, List<String> termosUsados, String descricaoUsuario,
                                    boolean[] geminiEsgotado, boolean[] mistralEsgotado) {
        String pedido = "Título: " + noticia.getTitulo()
                + (!termosUsados.isEmpty()
                        ? "\nTermos já usados na capa recentemente (não repita nenhum): " + String.join(", ", termosUsados)
                        : "")
                + (descricaoUsuario != null && !descricaoUsuario.isBlank()
                        ? "\nDescrição adicional do usuário para guiar o ícone: " + descricaoUsuario.trim()
                        : "");

        if (!geminiEsgotado[0]) {
            try {
                String json = geminiClient.chat(PROMPT_ICONE, pedido, SCHEMA_ICONE);
                return Acabamento.doJson(objectMapper, json).termoIcone();
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, passando a usar o Mistral para o novo ícone da capa: {}",
                        e.getMessage());
                geminiEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao decidir o novo ícone da capa para a notícia {} via Gemini, usando padrão: {}",
                        noticia.getId(), e.getMessage());
                return Acabamento.padrao(noticia).termoIcone();
            }
        }

        if (!mistralEsgotado[0]) {
            try {
                String json = mistralClient.chat(PROMPT_ICONE_MISTRAL, pedido, true);
                return Acabamento.doJson(objectMapper, json).termoIcone();
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Limite do Mistral atingido, usando termo de ícone padrão: {}", e.getMessage());
                mistralEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao decidir o novo ícone da capa para a notícia {} via Mistral, usando padrão: {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return Acabamento.padrao(noticia).termoIcone();
    }

    /**
     * Prefere o Gemini para condensar o texto do post; quando a cota diária dele estourar, passa a
     * usar o Mistral pelo resto da execução. Só devolve o texto revisado sem condensar se as duas IAs
     * estiverem indisponíveis.
     */
    private String gerarTextoIlustrado(Noticia noticia, boolean[] geminiEsgotado, boolean[] mistralEsgotado) {
        String pergunta = "Título: " + noticia.getTitulo() + "\nTexto revisado:\n" + noticia.getTextoRevisado();

        if (!geminiEsgotado[0]) {
            try {
                return geminiClient.chat(PROMPT_TEXTO, pergunta);
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, passando a usar o Mistral para condensar o texto do post: {}",
                        e.getMessage());
                geminiEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao gerar texto do post para a notícia {} via Gemini (usando texto revisado sem condensar): {}",
                        noticia.getId(), e.getMessage());
                return noticia.getTextoRevisado();
            }
        }

        if (!mistralEsgotado[0]) {
            try {
                return mistralClient.chat(PROMPT_TEXTO, pergunta);
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Limite do Mistral atingido, usando o texto revisado sem condensar: {}", e.getMessage());
                mistralEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao gerar texto do post para a notícia {} via Mistral (usando texto revisado sem condensar): {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return noticia.getTextoRevisado();
    }

    /**
     * Monta a capa: pede o acabamento (termo do ícone + cor de destaque) ao Gemini, busca o ícone no
     * Iconify e renderiza no template fixo que reproduz a identidade visual do blog (fundo
     * quadriculado escuro + ícone grande com brilho neon, sem título desenhado na imagem). Cada etapa
     * tem um fallback seguro, então a ilustração praticamente nunca falha por completo.
     */
    private String gerarIlustracao(Noticia noticia, boolean[] geminiEsgotado, boolean[] mistralEsgotado) {
        Acabamento acabamento = gerarAcabamento(noticia, geminiEsgotado, mistralEsgotado);
        String iconeSvg = buscarIconeComFallback(acabamento.termoIcone());
        noticia.setTermosIconeUsados(objectMapper.writeValueAsString(List.of(acabamento.termoIcone())));
        return renderizar(acabamento, iconeSvg);
    }

    /**
     * Prefere o Gemini; quando a cota diária dele estourar, passa a pedir o acabamento ao Mistral
     * (que ainda escolhe o termo do ícone de acordo com o tema da notícia) pelo resto da execução.
     * Só cai no acabamento fixo se as duas IAs estiverem indisponíveis.
     */
    private Acabamento gerarAcabamento(Noticia noticia, boolean[] geminiEsgotado, boolean[] mistralEsgotado) {
        String pedido = "Título: " + noticia.getTitulo();

        if (!geminiEsgotado[0]) {
            try {
                String json = geminiClient.chat(PROMPT_ACABAMENTO, pedido, SCHEMA_ACABAMENTO);
                return Acabamento.doJson(objectMapper, json);
            } catch (LimiteGeminiAtingidoException e) {
                log.warn("Limite do Gemini atingido, passando a usar o Mistral para o acabamento da capa: {}",
                        e.getMessage());
                geminiEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao decidir o acabamento da capa para a notícia {} via Gemini, usando padrão: {}",
                        noticia.getId(), e.getMessage());
                return Acabamento.padrao(noticia);
            }
        }

        if (!mistralEsgotado[0]) {
            try {
                String json = mistralClient.chat(PROMPT_ACABAMENTO_MISTRAL, pedido, true);
                return Acabamento.doJson(objectMapper, json);
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Limite do Mistral atingido, usando acabamento padrão: {}", e.getMessage());
                mistralEsgotado[0] = true;
            } catch (Exception e) {
                log.warn("Falha ao decidir o acabamento da capa para a notícia {} via Mistral, usando padrão: {}",
                        noticia.getId(), e.getMessage());
            }
        }
        return Acabamento.padrao(noticia);
    }

    private String buscarIconeComFallback(String termoIcone) {
        try {
            return iconifyClient.buscarIconeSvg(termoIcone, BIBLIOTECA_ICONES);
        } catch (Exception e) {
            log.warn("Falha ao buscar ícone \"{}\" no Iconify, usando ícone genérico: {}", termoIcone, e.getMessage());
            return ICONE_FALLBACK_CPU;
        }
    }

    /** Cor de traço fixa dos ícones desenhados à mão do blog (var(--ink) do line-art, #8CF7FF). */
    private static final String COR_ICONE_BLOG = "#8CF7FF";

    private static final double ICONE_CX = 600;
    private static final double ICONE_CY = 313;
    private static final double ICONE_TAMANHO = 420;

    /**
     * Reproduz a capa exatamente como o bloco ".illustration" do blog: fundo escuro quadriculado
     * (var(--card-2) + linhas em ciano), o ícone grande e centralizado sempre no ciano claro do
     * line-art do blog, com brilho neon e dois pontinhos de destaque na cor do tema — igual ao
     * acabamento dos ícones fixos (brain, cloud, terminal…) do blog. Sem título desenhado, já que o
     * post no blog mostra o título como texto ao lado da imagem.
     */
    private String renderizar(Acabamento acabamento, String iconeSvg) {
        String icone = IconeSvgUtil.posicionar(iconeSvg, ICONE_CX, ICONE_CY, ICONE_TAMANHO, COR_ICONE_BLOG);
        String corSecundaria = corSecundaria(acabamento.corDestaque());
        double meio = ICONE_TAMANHO / 2;
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 627">
                  <defs>
                    <pattern id="grade-blog" width="48" height="48" patternUnits="userSpaceOnUse">
                      <path d="M 48 0 L 0 0 0 48" fill="none" stroke="#00F0FF" stroke-opacity="0.16" stroke-width="1.6"/>
                    </pattern>
                    <filter id="brilho-neon" x="-60%%" y="-60%%" width="220%%" height="220%%">
                      <feGaussianBlur stdDeviation="9" result="blur"/>
                      <feMerge>
                        <feMergeNode in="blur"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>
                  <rect width="1200" height="627" fill="%s"/>
                  <rect width="1200" height="627" fill="url(#grade-blog)"/>
                  <line x1="0" y1="1" x2="1200" y2="1" stroke="#22222E" stroke-width="2"/>
                  <line x1="0" y1="626" x2="1200" y2="626" stroke="#22222E" stroke-width="2"/>
                  <g filter="url(#brilho-neon)">
                    %s
                    <circle cx="%s" cy="%s" r="9" fill="%s"/>
                    <circle cx="%s" cy="%s" r="6" fill="%s"/>
                  </g>
                </svg>
                """.formatted(COR_FUNDO_BLOG, icone,
                fmt(ICONE_CX + meio * 0.62), fmt(ICONE_CY - meio * 0.68), acabamento.corDestaque(),
                fmt(ICONE_CX - meio * 0.66), fmt(ICONE_CY + meio * 0.7), corSecundaria);
    }

    private static String corSecundaria(String corDestaque) {
        int indice = Math.max(0, CORES_BLOG.indexOf(corDestaque));
        return CORES_BLOG.get((indice + 2) % CORES_BLOG.size());
    }

    private static String fmt(double valor) {
        return String.valueOf(Math.round(valor * 100) / 100.0);
    }

    private record Acabamento(String termoIcone, String corDestaque) {

        static Acabamento doJson(ObjectMapper mapper, String json) {
            Map<?, ?> dados = mapper.readValue(json, Map.class);
            String termoIcone = textoSeguro(dados.get("termoIcone"), "chip");
            String destaque = corBlogSegura(dados.get("corDestaque"));
            return new Acabamento(termoIcone, destaque);
        }

        static Acabamento padrao(Noticia noticia) {
            long semente = noticia.getId() != null ? noticia.getId() : System.currentTimeMillis();
            String destaque = CORES_BLOG.get((int) (Math.abs(semente) % CORES_BLOG.size()));
            return new Acabamento("chip", destaque);
        }

        private static String textoSeguro(Object valor, String padrao) {
            return valor instanceof String s && !s.isBlank() ? s.trim() : padrao;
        }

        private static String corBlogSegura(Object valor) {
            String texto = valor instanceof String s ? s.trim().toUpperCase() : "";
            return CORES_BLOG.stream().anyMatch(c -> c.equalsIgnoreCase(texto)) ? texto : CORES_BLOG.get(0);
        }
    }
}
