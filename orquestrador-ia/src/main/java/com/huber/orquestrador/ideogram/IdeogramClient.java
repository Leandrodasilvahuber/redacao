package com.huber.orquestrador.ideogram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

@Component
public class IdeogramClient {

    private final RestClient restClient;
    private final IdeogramProperties properties;
    private final IdeogramLimiter limiter;
    private final ConfiguracaoService configuracaoService;

    public IdeogramClient(IdeogramProperties properties, IdeogramLimiter limiter,
                           ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.limiter = limiter;
        this.configuracaoService = configuracaoService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15_000);
        requestFactory.setReadTimeout(60_000);
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private String resolverApiKey() {
        String apiKey = configuracaoService.getIdeogramApiKey();
        return apiKey != null && !apiKey.isBlank() ? apiKey : properties.getApiKey();
    }

    /**
     * Gera uma imagem via Ideogram 3.0 e devolve como data URI base64. A resolução precisa ser uma das
     * combinações suportadas pela API (ver documentação); a mais próxima de 1200x627 é 1344x704.
     */
    public String gerarImagemBase64(String prompt, String negativePrompt, String resolucao, long seed) {
        limiter.reservarRequisicao();

        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("prompt", prompt);
        corpo.add("resolution", resolucao);
        corpo.add("rendering_speed", "TURBO");
        corpo.add("style_type", "DESIGN");
        corpo.add("num_images", "1");
        corpo.add("seed", String.valueOf(Math.abs(seed) % 2_000_000_000));
        if (negativePrompt != null && !negativePrompt.isBlank()) {
            corpo.add("negative_prompt", negativePrompt);
        }

        GerarResposta resposta;
        try {
            resposta = restClient.post()
                    .uri("/v1/ideogram-v3/generate")
                    .header("Api-Key", resolverApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(GerarResposta.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 402
                    || e.getStatusCode().value() == 401) {
                limiter.registrarLimiteRealAtingido();
                throw new LimiteIdeogramAtingidoException(
                        "Limite/saldo da conta Ideogram atingido (a API recusou a chamada): " + e.getMessage());
            }
            throw new FalhaIdeogramException("Falha ao gerar imagem no Ideogram: " + e.getMessage(), e);
        }

        if (resposta == null || resposta.data() == null || resposta.data().isEmpty()
                || resposta.data().get(0).url() == null) {
            throw new FalhaIdeogramException("Ideogram não retornou nenhuma imagem", null);
        }

        byte[] imagem;
        try {
            imagem = restClient.get()
                    .uri(resposta.data().get(0).url())
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            throw new FalhaIdeogramException("Falha ao baixar a imagem gerada pelo Ideogram: " + e.getMessage(), e);
        }

        if (imagem == null || imagem.length == 0) {
            throw new FalhaIdeogramException("Download da imagem do Ideogram veio vazio", null);
        }

        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imagem);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GerarResposta(List<ItemImagem> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ItemImagem(String url) {
    }
}
