package com.huber.orquestrador.gemini;

public record GeminiUsoStatus(
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
