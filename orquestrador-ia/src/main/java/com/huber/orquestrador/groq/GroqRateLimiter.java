package com.huber.orquestrador.groq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Impede que o app ultrapasse o plano gratuito do Groq: aplica sempre metade
 * dos limites reais do plano (FATOR_SEGURANCA), independente do que estiver
 * configurado em application.properties.
 */
@Component
public class GroqRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(GroqRateLimiter.class);
    private static final double FATOR_SEGURANCA = 0.5;
    private static final long JANELA_MINUTO_MS = 60_000;
    private static final long ESPERA_ENTRE_TENTATIVAS_MS = 2_000;

    private final int limiteRequisicoesPorMinuto;
    private final int limiteRequisicoesPorDia;
    private final int limiteTokensPorMinuto;
    private final int limiteTokensPorDia;

    private final Deque<Instant> requisicoesUltimoMinuto = new ArrayDeque<>();
    private final Deque<UsoToken> tokensUltimoMinuto = new ArrayDeque<>();

    private LocalDate diaAtual = LocalDate.now(ZoneId.systemDefault());
    private int requisicoesHoje = 0;
    private int tokensHoje = 0;

    public GroqRateLimiter(GroqLimiteProperties limitesDoPlano) {
        this.limiteRequisicoesPorMinuto = aplicarFator(limitesDoPlano.getRequisicoesPorMinuto());
        this.limiteRequisicoesPorDia = aplicarFator(limitesDoPlano.getRequisicoesPorDia());
        this.limiteTokensPorMinuto = aplicarFator(limitesDoPlano.getTokensPorMinuto());
        this.limiteTokensPorDia = aplicarFator(limitesDoPlano.getTokensPorDia());
        log.info("Limites efetivos do Groq (metade do plano): {} req/min, {} req/dia, {} tokens/min, {} tokens/dia",
                limiteRequisicoesPorMinuto, limiteRequisicoesPorDia, limiteTokensPorMinuto, limiteTokensPorDia);
    }

    private static int aplicarFator(int limiteDoPlano) {
        return Math.max(1, (int) Math.floor(limiteDoPlano * FATOR_SEGURANCA));
    }

    /**
     * Bloqueia a thread até haver folga na janela do último minuto.
     * Lança LimiteGroqAtingidoException se o limite diário já foi atingido.
     */
    public synchronized void reservarRequisicao() {
        renovarDiaSeNecessario();
        purgarJanelaMinuto();

        while (requisicoesUltimoMinuto.size() >= limiteRequisicoesPorMinuto
                || somaTokensUltimoMinuto() >= limiteTokensPorMinuto) {
            log.info("Aguardando janela de uso do Groq liberar (limite por minuto atingido)...");
            dormir();
            renovarDiaSeNecessario();
            purgarJanelaMinuto();
        }

        if (requisicoesHoje >= limiteRequisicoesPorDia || tokensHoje >= limiteTokensPorDia) {
            throw new LimiteGroqAtingidoException(
                    "Limite diário seguro do Groq atingido (%d req/dia ou %d tokens/dia, 50%% do plano). Tente novamente amanhã."
                            .formatted(limiteRequisicoesPorDia, limiteTokensPorDia));
        }

        requisicoesUltimoMinuto.add(Instant.now());
        requisicoesHoje++;
    }

    public synchronized void registrarTokensUsados(int tokens) {
        tokensUltimoMinuto.add(new UsoToken(Instant.now(), tokens));
        tokensHoje += tokens;
    }

    private void renovarDiaSeNecessario() {
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        if (!hoje.equals(diaAtual)) {
            diaAtual = hoje;
            requisicoesHoje = 0;
            tokensHoje = 0;
        }
    }

    private void purgarJanelaMinuto() {
        Instant limite = Instant.now().minusMillis(JANELA_MINUTO_MS);
        requisicoesUltimoMinuto.removeIf(instante -> instante.isBefore(limite));
        tokensUltimoMinuto.removeIf(uso -> uso.instante().isBefore(limite));
    }

    private long somaTokensUltimoMinuto() {
        return tokensUltimoMinuto.stream().mapToLong(UsoToken::tokens).sum();
    }

    private void dormir() {
        try {
            Thread.sleep(ESPERA_ENTRE_TENTATIVAS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido enquanto aguardava limite de uso do Groq", e);
        }
    }

    private record UsoToken(Instant instante, int tokens) {
    }
}
