package com.huber.orquestrador.mistral;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Acompanha o gasto estimado no crédito gratuito do Mistral (La Plateforme) e trava o uso na cota
 * (%) configurada em Configurações (padrão 50% do crédito total, ajustável de 0 a 100 a qualquer
 * momento) para nunca chegar perto de estourar o saldo real.
 *
 * O uso diário é persistido no banco (tabela mistral_uso_diario) para sobreviver a restart.
 */
@Component
public class MistralOrcamentoLimiter {

    private static final Logger log = LoggerFactory.getLogger(MistralOrcamentoLimiter.class);

    private final MistralOrcamentoProperties properties;
    private final MistralUsoDiarioRepository usoDiarioRepository;
    private final ConfiguracaoService configuracaoService;

    private MistralUsoDiario usoAtual;

    public MistralOrcamentoLimiter(MistralOrcamentoProperties properties, MistralUsoDiarioRepository usoDiarioRepository,
                                    ConfiguracaoService configuracaoService) {
        this.properties = properties;
        this.usoDiarioRepository = usoDiarioRepository;
        this.configuracaoService = configuracaoService;
        this.usoAtual = carregarOuCriar(LocalDate.now(ZoneId.systemDefault()));
        log.info("Limite seguro do Mistral ({}% do crédito de US$ {}): US$ {}. Gasto já registrado hoje: US$ {}.",
                configuracaoService.getCotaMistral(), properties.getCreditoUsd(), limiteSeguroUsd(), custoAtual());
    }

    private double limiteSeguroUsd() {
        double fator = Math.max(0, Math.min(100, configuracaoService.getCotaMistral())) / 100.0;
        return properties.getCreditoUsd() * fator;
    }

    private MistralUsoDiario carregarOuCriar(LocalDate dia) {
        return usoDiarioRepository.findById(dia).orElseGet(() -> usoDiarioRepository.save(new MistralUsoDiario(dia)));
    }

    private double custoAtual() {
        return custo(usoAtual.getTokensEntrada(), usoAtual.getTokensSaida());
    }

    private double custo(long tokensEntrada, long tokensSaida) {
        return (tokensEntrada / 1_000_000.0) * properties.getPrecoEntradaPorMilhao()
                + (tokensSaida / 1_000_000.0) * properties.getPrecoSaidaPorMilhao();
    }

    /**
     * Lança LimiteMistralAtingidoException se o gasto seguro do dia já foi atingido.
     */
    public synchronized void reservarRequisicao() {
        renovarDiaSeNecessario();
        double limiteSeguroUsd = limiteSeguroUsd();
        if (custoAtual() >= limiteSeguroUsd) {
            throw new LimiteMistralAtingidoException(
                    "Limite seguro do crédito do Mistral atingido (US$ %.2f de US$ %.2f, %d%% do crédito gratuito). Tente novamente amanhã ou com outra chave."
                            .formatted(limiteSeguroUsd, properties.getCreditoUsd(), configuracaoService.getCotaMistral()));
        }
        usoAtual.incrementarRequisicoes();
        usoDiarioRepository.save(usoAtual);
    }

    public synchronized void registrarTokensUsados(long tokensEntrada, long tokensSaida) {
        renovarDiaSeNecessario();
        usoAtual.somarTokens(tokensEntrada, tokensSaida);
        usoDiarioRepository.save(usoAtual);
    }

    /**
     * Chamado quando a própria API do Mistral recusa a chamada por limite/saldo real da conta.
     * Trava o contador local no teto seguro do dia para não tentar de novo até o dia virar,
     * mesmo que nosso contador de segurança (persistido) tivesse folga.
     */
    public synchronized void registrarLimiteRealAtingido() {
        renovarDiaSeNecessario();
        double deficitUsd = Math.max(0, limiteSeguroUsd() - custoAtual());
        if (deficitUsd > 0) {
            double precoPorMilhao = properties.getPrecoSaidaPorMilhao() > 0
                    ? properties.getPrecoSaidaPorMilhao()
                    : properties.getPrecoEntradaPorMilhao();
            if (precoPorMilhao > 0) {
                long tokensParaSaturar = (long) Math.ceil(deficitUsd / precoPorMilhao * 1_000_000.0);
                usoAtual.somarTokens(0, tokensParaSaturar);
            }
        }
        usoDiarioRepository.save(usoAtual);
    }

    public synchronized MistralUsoStatus statusAtual() {
        renovarDiaSeNecessario();
        return new MistralUsoStatus(
                usoAtual.getRequisicoes(),
                usoAtual.getTokensEntrada(),
                usoAtual.getTokensSaida(),
                custoAtual(),
                limiteSeguroUsd(),
                properties.getCreditoUsd());
    }

    private void renovarDiaSeNecessario() {
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        if (!hoje.equals(usoAtual.getDia())) {
            usoAtual = carregarOuCriar(hoje);
        }
    }
}
