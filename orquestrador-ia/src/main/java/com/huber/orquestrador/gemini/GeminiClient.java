package com.huber.orquestrador.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final GeminiRateLimiter rateLimiter;
    private final ConfiguracaoService configuracaoService;

    public GeminiClient(GeminiProperties properties, GeminiRateLimiter rateLimiter,
                         ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.configuracaoService = configuracaoService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    private String resolverApiKey() {
        String apiKey = configuracaoService.getGeminiApiKey();
        return apiKey != null && !apiKey.isBlank() ? apiKey : properties.getApiKey();
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    public String chat(String systemPrompt, String userPrompt, Map<String, Object> schemaJson) {
        rateLimiter.reservarRequisicao();

        GenerationConfig config = schemaJson != null
                ? new GenerationConfig("application/json", schemaJson)
                : null;

        GenerateRequest request = new GenerateRequest(
                new Conteudo(List.of(new Parte(systemPrompt))),
                List.of(new Conteudo(List.of(new Parte(userPrompt)))),
                config
        );

        GenerateResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent?key={apiKey}", properties.getModel(), resolverApiKey())
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            rateLimiter.registrarLimiteRealAtingido();
            throw new LimiteGeminiAtingidoException(
                    "Limite real da conta Gemini atingido (a API recusou a chamada): " + e.getMessage());
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini não retornou nenhuma resposta");
        }

        if (response.usageMetadata() != null) {
            rateLimiter.registrarTokensUsados(response.usageMetadata().totalTokenCount());
        }

        return response.candidates().get(0).content().parts().get(0).text().trim();
    }

    private record Parte(String text) {
    }

    private record Conteudo(List<Parte> parts) {
    }

    private record GenerationConfig(@JsonProperty("responseMimeType") String responseMimeType,
                                     @JsonProperty("responseSchema") Map<String, Object> responseSchema) {
    }

    private record GenerateRequest(@JsonProperty("systemInstruction") Conteudo systemInstruction,
                                    List<Conteudo> contents,
                                    @JsonProperty("generationConfig") GenerationConfig generationConfig) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenerateResponse(List<Candidato> candidates,
                                     @JsonProperty("usageMetadata") UsageMetadata usageMetadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidato(Conteudo content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsageMetadata(@JsonProperty("totalTokenCount") int totalTokenCount) {
    }
}
