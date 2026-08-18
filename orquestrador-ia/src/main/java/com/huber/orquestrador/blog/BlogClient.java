package com.huber.orquestrador.blog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BlogClient {

    private final RestClient.Builder restClientBuilder;
    private final BlogProperties properties;
    private final ConfiguracaoService configuracaoService;

    public BlogClient(BlogProperties properties, ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.configuracaoService = configuracaoService;
        this.restClientBuilder = RestClient.builder();
    }

    private String resolverApiUrl() {
        String apiUrl = configuracaoService.getBlogApiUrl();
        return apiUrl != null && !apiUrl.isBlank() ? apiUrl : properties.getApiUrl();
    }

    private String resolverApiToken() {
        String apiToken = configuracaoService.getBlogApiToken();
        return apiToken != null && !apiToken.isBlank() ? apiToken : properties.getApiToken();
    }

    private RestClient restClient() {
        return restClientBuilder.baseUrl(resolverApiUrl() + "/api/adm").build();
    }

    public String criarPost(PostRequest post) {
        PostResponse resposta = restClient().post()
                .uri("/posts")
                .header("Authorization", "Bearer " + resolverApiToken())
                .body(post)
                .retrieve()
                .body(PostResponse.class);
        if (resposta == null || resposta.id() == null) {
            throw new IllegalStateException("O blog não retornou o id do post criado");
        }
        return String.valueOf(resposta.id());
    }

    public void excluirPost(String id) {
        restClient().delete()
                .uri("/posts/{id}", id)
                .header("Authorization", "Bearer " + resolverApiToken())
                .retrieve()
                .toBodilessEntity();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PostRequest(
            String role,
            String illustration,
            String lead,
            String body,
            String tags,
            @JsonProperty("cover_image_base64") String coverImageBase64,
            @JsonProperty("source_url") String sourceUrl,
            Integer likes,
            Integer comments,
            Integer reposts,
            @JsonProperty("top_reactor") String topReactor,
            @JsonProperty("comment_name") String commentName,
            @JsonProperty("comment_role") String commentRole,
            @JsonProperty("comment_text") String commentText,
            @JsonProperty("published_at") String publishedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PostResponse(Long id) {
    }
}
