package com.huber.orquestrador.gemini;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Impede que o app ultrapasse o plano gratuito do Gemini: aplica sobre os limites reais do plano a
 * cota (%) configurada em Configurações (padrão 50%, ajustável de 0 a 100 a qualquer momento, sem
 * precisar reiniciar o app), independente do que estiver em application.properties.
 *
 * O uso diário (requisições e tokens) é persistido no banco (tabela
 * gemini_uso_diario) para que reiniciar o app não zere o contador de segurança.
 */
@Component
public class GeminiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(GeminiRateLimiter.class);
    private static final long JANELA_MINUTO_MS = 60_000;
    private static final long ESPERA_ENTRE_TENTATIVAS_MS = 2_000;

    private final int limiteRequisicoesPorMinutoPlano;
    private final int limiteRequisicoesPorDiaPlano;
    private final int limiteTokensPorMinutoPlano;
    private final int limiteTokensPorDiaPlano;

    private final GeminiUsoDiarioRepository usoDiarioRepository;
    private final ConfiguracaoService configuracaoService;

    private final Deque<Instant> requisicoesUltimoMinuto = new ArrayDeque<>();
    private final Deque<UsoToken> tokensUltimoMinuto = new ArrayDeque<>();

    private GeminiUsoDiario usoAtual;

    public GeminiRateLimiter(GeminiLimiteProperties limitesDoPlano, GeminiUsoDiarioRepository usoDiarioRepository,
                              ConfiguracaoService configuracaoService) {
        this.limiteRequisicoesPorMinutoPlano = limitesDoPlano.getRequisicoesPorMinuto();
        this.limiteRequisicoesPorDiaPlano = limitesDoPlano.getRequisicoesPorDia();
        this.limiteTokensPorMinutoPlano = limitesDoPlano.getTokensPorMinuto();
        this.limiteTokensPorDiaPlano = limitesDoPlano.getTokensPorDia();
        this.usoDiarioRepository = usoDiarioRepository;
        this.configuracaoService = configuracaoService;
        this.usoAtual = carregarOuCriar(LocalDate.now(ZoneId.systemDefault()));
        log.info("Limites efetivos do Gemini ({}% do plano): {} req/min, {} req/dia, {} tokens/min, {} tokens/dia. "
                        + "Uso já registrado hoje: {} req, {} tokens.",
                configuracaoService.getCotaGemini(), limiteRequisicoesPorMinuto(), limiteRequisicoesPorDia(),
                limiteTokensPorMinuto(), limiteTokensPorDia(), usoAtual.getRequisicoes(), usoAtual.getTokens());
    }

    private int aplicarCota(int limiteDoPlano) {
        double fator = Math.max(0, Math.min(100, configuracaoService.getCotaGemini())) / 100.0;
        return Math.max(1, (int) Math.floor(limiteDoPlano * fator));
    }

    private int limiteRequisicoesPorMinuto() {
        return aplicarCota(limiteRequisicoesPorMinutoPlano);
    }

    private int limiteRequisicoesPorDia() {
        return aplicarCota(limiteRequisicoesPorDiaPlano);
    }

    private int limiteTokensPorMinuto() {
        return aplicarCota(limiteTokensPorMinutoPlano);
    }

    private int limiteTokensPorDia() {
        return aplicarCota(limiteTokensPorDiaPlano);
    }

    private GeminiUsoDiario carregarOuCriar(LocalDate dia) {
        return usoDiarioRepository.findById(dia).orElseGet(() -> usoDiarioRepository.save(new GeminiUsoDiario(dia)));
    }

    /**
     * Bloqueia a thread até haver folga na janela do último minuto.
     * Lança LimiteGeminiAtingidoException se o limite diário já foi atingido.
     */
    public synchronized void reservarRequisicao() {
        renovarDiaSeNecessario();
        purgarJanelaMinuto();

        while (requisicoesUltimoMinuto.size() >= limiteRequisicoesPorMinuto()
                || somaTokensUltimoMinuto() >= limiteTokensPorMinuto()) {
            log.info("Aguardando janela de uso do Gemini liberar (limite por minuto atingido)...");
            dormir();
            renovarDiaSeNecessario();
            purgarJanelaMinuto();
        }

        if (usoAtual.getRequisicoes() >= limiteRequisicoesPorDia() || usoAtual.getTokens() >= limiteTokensPorDia()) {
            throw new LimiteGeminiAtingidoException(
                    "Limite diário seguro do Gemini atingido (%d req/dia ou %d tokens/dia, %d%% do plano). Tente novamente amanhã."
                            .formatted(limiteRequisicoesPorDia(), limiteTokensPorDia(), configuracaoService.getCotaGemini()));
        }

        requisicoesUltimoMinuto.add(Instant.now());
        usoAtual.incrementarRequisicoes();
        usoDiarioRepository.save(usoAtual);
    }

    /**
     * Chamado quando a própria API do Gemini recusa a chamada por limite real de uso.
     * Trava o contador local no teto do dia para não tentar de novo até o dia virar,
     * mesmo que nosso contador de segurança (persistido) tivesse folga.
     */
    public synchronized void registrarLimiteRealAtingido() {
        renovarDiaSeNecessario();
        usoAtual.somarTokens(Math.max(0, limiteTokensPorDia() - usoAtual.getTokens()));
        usoDiarioRepository.save(usoAtual);
    }

    public synchronized void registrarTokensUsados(int tokens) {
        tokensUltimoMinuto.add(new UsoToken(Instant.now(), tokens));
        usoAtual.somarTokens(tokens);
        usoDiarioRepository.save(usoAtual);
    }

    public synchronized GeminiUsoStatus statusAtual() {
        renovarDiaSeNecessario();
        purgarJanelaMinuto();
        return new GeminiUsoStatus(
                usoAtual.getRequisicoes(),
                limiteRequisicoesPorDia(),
                usoAtual.getTokens(),
                limiteTokensPorDia(),
                requisicoesUltimoMinuto.size(),
                limiteRequisicoesPorMinuto(),
                (int) somaTokensUltimoMinuto(),
                limiteTokensPorMinuto());
    }

    private void renovarDiaSeNecessario() {
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        if (!hoje.equals(usoAtual.getDia())) {
            usoAtual = carregarOuCriar(hoje);
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
            throw new IllegalStateException("Interrompido enquanto aguardava limite de uso do Gemini", e);
        }
    }

    private record UsoToken(Instant instante, int tokens) {
    }
}
