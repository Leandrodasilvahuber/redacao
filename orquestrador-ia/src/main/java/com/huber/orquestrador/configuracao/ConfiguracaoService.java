package com.huber.orquestrador.configuracao;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConfiguracaoService {

    private static final String MASCARA = "••••";

    private final ConfiguracaoRepository configuracaoRepository;

    public ConfiguracaoService(ConfiguracaoRepository configuracaoRepository) {
        this.configuracaoRepository = configuracaoRepository;
    }

    public Configuracao obter() {
        return configuracaoRepository.findById(Configuracao.ID_UNICO)
                .orElseGet(() -> configuracaoRepository.save(new Configuracao(Configuracao.ID_UNICO)));
    }

    public ConfiguracaoDTO.Response obterResposta() {
        Configuracao config = obter();
        return new ConfiguracaoDTO.Response(
                mascarar(config.getGroqApiKey()), temValor(config.getGroqApiKey()),
                mascarar(config.getGeminiApiKey()), temValor(config.getGeminiApiKey()),
                mascarar(config.getMistralApiKey()), temValor(config.getMistralApiKey()),
                parseCriterios(config.getCriteriosBusca()).stream().map(Enum::name).collect(Collectors.toList()),
                config.isRevisarFonteVeridica(),
                config.isRevisarEstrutura(),
                config.isRevisarPadraoLinkedin(),
                config.isAtribuirFonte(),
                config.getBlogApiUrl(),
                mascarar(config.getBlogApiToken()), temValor(config.getBlogApiToken()),
                getBibliotecaIcones().name(),
                mascarar(config.getLinkedinClientId()), temValor(config.getLinkedinClientId()),
                mascarar(config.getLinkedinClientSecret()), temValor(config.getLinkedinClientSecret()),
                temValor(config.getLinkedinAccessToken()),
                config.getLinkedinPersonUrn(),
                config.getLinkedinTokenExpiraEm() != null ? config.getLinkedinTokenExpiraEm().toString() : null
        );
    }

    public ConfiguracaoDTO.Response salvar(ConfiguracaoDTO.Request request) {
        Configuracao config = obter();

        if (temValor(request.groqApiKey())) {
            config.setGroqApiKey(request.groqApiKey().trim());
        }
        if (temValor(request.geminiApiKey())) {
            config.setGeminiApiKey(request.geminiApiKey().trim());
        }
        if (temValor(request.mistralApiKey())) {
            config.setMistralApiKey(request.mistralApiKey().trim());
        }

        List<String> criterios = request.criteriosBusca() != null ? request.criteriosBusca() : List.of();
        String criteriosValidos = criterios.stream()
                .filter(nome -> parseCriterioSeguro(nome) != null)
                .distinct()
                .collect(Collectors.joining(","));
        config.setCriteriosBusca(criteriosValidos);

        config.setRevisarFonteVeridica(request.revisarFonteVeridica());
        config.setRevisarEstrutura(request.revisarEstrutura());
        config.setRevisarPadraoLinkedin(request.revisarPadraoLinkedin());
        config.setAtribuirFonte(request.atribuirFonte());

        if (temValor(request.blogApiUrl())) {
            config.setBlogApiUrl(request.blogApiUrl().trim());
        }
        if (temValor(request.blogApiToken())) {
            config.setBlogApiToken(request.blogApiToken().trim());
        }
        BibliotecaIcones bibliotecaIcones = parseBibliotecaIconesSegura(request.bibliotecaIcones());
        config.setBibliotecaIcones(bibliotecaIcones != null ? bibliotecaIcones : BibliotecaIcones.MATERIAL_SYMBOLS);

        if (temValor(request.linkedinClientId())) {
            config.setLinkedinClientId(request.linkedinClientId().trim());
        }
        if (temValor(request.linkedinClientSecret())) {
            config.setLinkedinClientSecret(request.linkedinClientSecret().trim());
        }

        config.marcarAtualizada();
        configuracaoRepository.save(config);
        return obterResposta();
    }

    public void salvarTokenLinkedin(String accessToken, Instant expiraEm, String personUrn) {
        Configuracao config = obter();
        config.setLinkedinAccessToken(accessToken);
        config.setLinkedinTokenExpiraEm(expiraEm);
        config.setLinkedinPersonUrn(personUrn);
        config.marcarAtualizada();
        configuracaoRepository.save(config);
    }

    public List<CriterioBusca> getCriteriosBuscaAtivos() {
        return parseCriterios(obter().getCriteriosBusca());
    }

    public boolean isRevisarFonteVeridica() {
        return obter().isRevisarFonteVeridica();
    }

    public boolean isRevisarEstrutura() {
        return obter().isRevisarEstrutura();
    }

    public boolean isRevisarPadraoLinkedin() {
        return obter().isRevisarPadraoLinkedin();
    }

    public boolean isAtribuirFonte() {
        return obter().isAtribuirFonte();
    }

    public String getGroqApiKey() {
        return obter().getGroqApiKey();
    }

    public String getGeminiApiKey() {
        return obter().getGeminiApiKey();
    }

    public String getMistralApiKey() {
        return obter().getMistralApiKey();
    }

    public String getBlogApiUrl() {
        return obter().getBlogApiUrl();
    }

    public String getBlogApiToken() {
        return obter().getBlogApiToken();
    }

    public BibliotecaIcones getBibliotecaIcones() {
        BibliotecaIcones biblioteca = obter().getBibliotecaIcones();
        return biblioteca != null ? biblioteca : BibliotecaIcones.MATERIAL_SYMBOLS;
    }

    public String getLinkedinClientId() {
        return obter().getLinkedinClientId();
    }

    public String getLinkedinClientSecret() {
        return obter().getLinkedinClientSecret();
    }

    public String getLinkedinAccessToken() {
        return obter().getLinkedinAccessToken();
    }

    public Instant getLinkedinTokenExpiraEm() {
        return obter().getLinkedinTokenExpiraEm();
    }

    public String getLinkedinPersonUrn() {
        return obter().getLinkedinPersonUrn();
    }

    private List<CriterioBusca> parseCriterios(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(this::parseCriterioSeguro)
                .filter(criterio -> criterio != null)
                .collect(Collectors.toList());
    }

    private CriterioBusca parseCriterioSeguro(String nome) {
        try {
            return CriterioBusca.valueOf(nome.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BibliotecaIcones parseBibliotecaIconesSegura(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return BibliotecaIcones.valueOf(nome.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean temValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String mascarar(String valor) {
        if (!temValor(valor)) {
            return "";
        }
        if (valor.length() <= 4) {
            return MASCARA;
        }
        return MASCARA + valor.substring(valor.length() - 4);
    }
}
