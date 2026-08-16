package com.huber.orquestrador.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq.limite")
public class GroqLimiteProperties {

    private int requisicoesPorMinuto;
    private int requisicoesPorDia;
    private int tokensPorMinuto;
    private int tokensPorDia;

    public int getRequisicoesPorMinuto() {
        return requisicoesPorMinuto;
    }

    public void setRequisicoesPorMinuto(int requisicoesPorMinuto) {
        this.requisicoesPorMinuto = requisicoesPorMinuto;
    }

    public int getRequisicoesPorDia() {
        return requisicoesPorDia;
    }

    public void setRequisicoesPorDia(int requisicoesPorDia) {
        this.requisicoesPorDia = requisicoesPorDia;
    }

    public int getTokensPorMinuto() {
        return tokensPorMinuto;
    }

    public void setTokensPorMinuto(int tokensPorMinuto) {
        this.tokensPorMinuto = tokensPorMinuto;
    }

    public int getTokensPorDia() {
        return tokensPorDia;
    }

    public void setTokensPorDia(int tokensPorDia) {
        this.tokensPorDia = tokensPorDia;
    }
}
