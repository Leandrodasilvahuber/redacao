package com.huber.orquestrador.flux;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.net.URLEncoder;

@Component
public class FluxClient {

    private final RestClient restClient;

    public FluxClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15_000);
        requestFactory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://image.pollinations.ai")
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Gera uma imagem via Flux Schnell (Pollinations.ai, sem chave de API) e devolve como data URI base64.
     */
    public String gerarImagemBase64(String prompt, String negativePrompt, int largura, int altura, long seed) {
        String promptCodificado = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        String uri = "/prompt/" + promptCodificado
                + "?width=" + largura + "&height=" + altura
                + "&model=flux&seed=" + seed + "&nologo=true";
        if (negativePrompt != null && !negativePrompt.isBlank()) {
            uri += "&negative_prompt=" + URLEncoder.encode(negativePrompt, StandardCharsets.UTF_8).replace("+", "%20");
        }

        byte[] imagem;
        try {
            imagem = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            throw new FalhaFluxException("Falha ao gerar imagem no Flux (Pollinations): " + e.getMessage(), e);
        }

        if (imagem == null || imagem.length == 0) {
            throw new FalhaFluxException("Flux (Pollinations) não retornou nenhuma imagem", null);
        }

        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imagem);
    }
}
