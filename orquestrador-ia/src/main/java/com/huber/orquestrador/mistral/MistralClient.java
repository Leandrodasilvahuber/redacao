package com.huber.orquestrador.mistral;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MistralClient {

    private final RestClient restClient;
    private final MistralProperties properties;
    private final MistralOrcamentoLimiter orcamentoLimiter;
    private final ConfiguracaoService configuracaoService;

    public MistralClient(MistralProperties properties, MistralOrcamentoLimiter orcamentoLimiter,
                          ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.orcamentoLimiter = orcamentoLimiter;
        this.configuracaoService = configuracaoService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    private String resolverApiKey() {
        String apiKey = configuracaoService.getMistralApiKey();
        return apiKey != null && !apiKey.isBlank() ? apiKey : properties.getApiKey();
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, false);
    }

    public String chat(String systemPrompt, String userPrompt, boolean respostaJson) {
        orcamentoLimiter.reservarRequisicao();

        ChatRequest request = new ChatRequest(
                properties.getModel(),
                List.of(
                        new Mensagem("system", systemPrompt),
                        new Mensagem("user", userPrompt)
                ),
                0.5,
                respostaJson ? new ResponseFormat("json_object") : null
        );

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + resolverApiKey())
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 402) {
                orcamentoLimiter.registrarLimiteRealAtingido();
                throw new LimiteMistralAtingidoException(
                        "Limite/saldo da conta Mistral atingido (a API recusou a chamada): " + e.getMessage());
            }
            throw e;
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Mistral não retornou nenhuma resposta");
        }

        if (response.usage() != null) {
            orcamentoLimiter.registrarTokensUsados(response.usage().promptTokens(), response.usage().completionTokens());
        }

        return response.choices().get(0).message().content().trim();
    }

    private record Mensagem(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatRequest(String model, List<Mensagem> messages, double temperature,
                                @JsonProperty("response_format") ResponseFormat responseFormat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Mensagem message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(@JsonProperty("prompt_tokens") long promptTokens,
                          @JsonProperty("completion_tokens") long completionTokens) {
    }
}
