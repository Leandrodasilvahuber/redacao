package com.huber.orquestrador.linkedin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LinkedInClient {

    private final RestClient restClient;
    private final RestClient authRestClient;
    private final LinkedInProperties properties;
    private final ConfiguracaoService configuracaoService;

    public LinkedInClient(LinkedInProperties properties, ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.configuracaoService = configuracaoService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.linkedin.com")
                .build();
        this.authRestClient = RestClient.builder()
                .baseUrl("https://www.linkedin.com")
                .build();
    }

    private String resolverClientId() {
        String clientId = configuracaoService.getLinkedinClientId();
        return clientId != null && !clientId.isBlank() ? clientId : properties.getClientId();
    }

    private String resolverClientSecret() {
        String clientSecret = configuracaoService.getLinkedinClientSecret();
        return clientSecret != null && !clientSecret.isBlank() ? clientSecret : properties.getClientSecret();
    }

    public String construirUrlAutorizacao(String state) {
        return "https://www.linkedin.com/oauth/v2/authorization"
                + "?response_type=code"
                + "&client_id=" + resolverClientId()
                + "&redirect_uri=" + properties.getRedirectUri()
                + "&scope=openid%20profile%20w_member_social"
                + "&state=" + state;
    }

    public TrocaTokenResponse trocarCodigoPorToken(String code) {
        MultiValueMap<String, String> corpo = new LinkedMultiValueMap<>();
        corpo.add("grant_type", "authorization_code");
        corpo.add("code", code);
        corpo.add("redirect_uri", properties.getRedirectUri());
        corpo.add("client_id", resolverClientId());
        corpo.add("client_secret", resolverClientSecret());

        return authRestClient.post()
                .uri("/oauth/v2/accessToken")
                .body(corpo)
                .retrieve()
                .body(TrocaTokenResponse.class);
    }

    public String obterPersonUrn(String accessToken) {
        UserInfoResponse resposta = restClient.get()
                .uri("/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(UserInfoResponse.class);
        if (resposta == null || resposta.sub() == null) {
            throw new IllegalStateException("LinkedIn não retornou o identificador do usuário");
        }
        return "urn:li:person:" + resposta.sub();
    }

    public String publicarPost(String accessToken, String personUrn, String texto, byte[] imagemPng) {
        String imagemUrn = imagemPng != null && imagemPng.length > 0
                ? registrarUploadImagem(accessToken, personUrn, imagemPng)
                : null;

        Map<String, Object> corpo = new HashMap<>(Map.of(
                "author", personUrn,
                "commentary", texto,
                "visibility", "PUBLIC",
                "distribution", Map.of(
                        "feedDistribution", "MAIN_FEED",
                        "targetEntities", List.of(),
                        "thirdPartyDistributionChannels", List.of()
                ),
                "lifecycleState", "PUBLISHED",
                "isReshareDisabledByAuthor", false
        ));
        if (imagemUrn != null) {
            corpo.put("content", Map.of("media", Map.of("id", imagemUrn)));
        }

        return restClient.post()
                .uri("/rest/posts")
                .header("Authorization", "Bearer " + accessToken)
                .header("LinkedIn-Version", properties.getApiVersion())
                .header("X-Restli-Protocol-Version", "2.0.0")
                .body(corpo)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getFirst("x-restli-id");
    }

    public void excluirPost(String accessToken, String postUrn) {
        restClient.delete()
                .uri("/rest/posts/{urn}", postUrn)
                .header("Authorization", "Bearer " + accessToken)
                .header("LinkedIn-Version", properties.getApiVersion())
                .header("X-Restli-Protocol-Version", "2.0.0")
                .retrieve()
                .toBodilessEntity();
    }

    private String registrarUploadImagem(String accessToken, String personUrn, byte[] imagemPng) {
        Map<String, Object> corpo = Map.of(
                "initializeUploadRequest", Map.of("owner", personUrn)
        );
        InicializarUploadResponse resposta = restClient.post()
                .uri("/rest/images?action=initializeUpload")
                .header("Authorization", "Bearer " + accessToken)
                .header("LinkedIn-Version", properties.getApiVersion())
                .header("X-Restli-Protocol-Version", "2.0.0")
                .body(corpo)
                .retrieve()
                .body(InicializarUploadResponse.class);
        if (resposta == null || resposta.value() == null) {
            throw new IllegalStateException("LinkedIn não retornou os dados de upload da imagem");
        }

        restClient.put()
                .uri(resposta.value().uploadUrl())
                .header("Authorization", "Bearer " + accessToken)
                .body(imagemPng)
                .retrieve()
                .toBodilessEntity();

        return resposta.value().image();
    }

    public record TrocaTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresInSegundos
    ) {
        public Instant calcularExpiracao() {
            return Instant.now().plusSeconds(expiresInSegundos);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfoResponse(String sub) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InicializarUploadResponse(ValorUpload value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ValorUpload(String uploadUrl, String image) {
    }
}
