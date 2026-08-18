package com.huber.orquestrador.mistral;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mistral.orcamento")
public class MistralOrcamentoProperties {

    private double creditoUsd;
    private double precoEntradaPorMilhao;
    private double precoSaidaPorMilhao;

    public double getCreditoUsd() {
        return creditoUsd;
    }

    public void setCreditoUsd(double creditoUsd) {
        this.creditoUsd = creditoUsd;
    }

    public double getPrecoEntradaPorMilhao() {
        return precoEntradaPorMilhao;
    }

    public void setPrecoEntradaPorMilhao(double precoEntradaPorMilhao) {
        this.precoEntradaPorMilhao = precoEntradaPorMilhao;
    }

    public double getPrecoSaidaPorMilhao() {
        return precoSaidaPorMilhao;
    }

    public void setPrecoSaidaPorMilhao(double precoSaidaPorMilhao) {
        this.precoSaidaPorMilhao = precoSaidaPorMilhao;
    }
}
