package com.huber.orquestrador.groq;

public record GroqUsoStatus(
        int requisicoesHoje,
        int limiteRequisicoesPorDia,
        int tokensHoje,
        int limiteTokensPorDia,
        int requisicoesUltimoMinuto,
        int limiteRequisicoesPorMinuto,
        int tokensUltimoMinuto,
        int limiteTokensPorMinuto
) {
}
