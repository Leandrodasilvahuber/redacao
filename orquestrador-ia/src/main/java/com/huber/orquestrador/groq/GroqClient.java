package com.huber.orquestrador.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GroqClient {

    private final RestClient restClient;
    private final GroqProperties properties;
    private final GroqRateLimiter rateLimiter;
    private final ConfiguracaoService configuracaoService;

    public GroqClient(GroqProperties properties, GroqRateLimiter rateLimiter, ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.configuracaoService = configuracaoService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    private String resolverApiKey() {
        String apiKey = configuracaoService.getGroqApiKey();
        return apiKey != null && !apiKey.isBlank() ? apiKey : properties.getApiKey();
    }

    public String chat(String systemPrompt, String userPrompt) {
        rateLimiter.reservarRequisicao();

        ChatRequest request = new ChatRequest(
                properties.getModel(),
                List.of(
                        new Mensagem("system", systemPrompt),
                        new Mensagem("user", userPrompt)
                ),
                0.5
        );

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + resolverApiKey())
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            rateLimiter.registrarLimiteRealAtingido();
            throw new LimiteGroqAtingidoException(
                    "Limite real da conta Groq atingido (a Groq recusou a chamada): " + e.getMessage());
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Groq não retornou nenhuma resposta");
        }

        if (response.usage() != null) {
            rateLimiter.registrarTokensUsados(response.usage().totalTokens());
        }

        return response.choices().get(0).message().content().trim();
    }

    private record Mensagem(String role, String content) {
    }

    private record ChatRequest(String model, List<Mensagem> messages, double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Mensagem message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(@JsonProperty("total_tokens") int totalTokens) {
    }
}
