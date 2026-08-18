package com.huber.orquestrador.ideogram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ideogram")
public class IdeogramProperties {

    private String apiKey;
    private String baseUrl;
    private int limiteImagensPorDia;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getLimiteImagensPorDia() {
        return limiteImagensPorDia;
    }

    public void setLimiteImagensPorDia(int limiteImagensPorDia) {
        this.limiteImagensPorDia = limiteImagensPorDia;
    }
}
