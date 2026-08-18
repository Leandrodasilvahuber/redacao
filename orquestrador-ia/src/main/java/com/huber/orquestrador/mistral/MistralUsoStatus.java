package com.huber.orquestrador.mistral;

public record MistralUsoStatus(
        int requisicoesHoje,
        long tokensEntradaHoje,
        long tokensSaidaHoje,
        double custoHojeUsd,
        double limiteSeguroUsd,
        double creditoTotalUsd
) {
}
