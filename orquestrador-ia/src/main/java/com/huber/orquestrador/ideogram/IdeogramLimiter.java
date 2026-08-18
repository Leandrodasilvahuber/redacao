package com.huber.orquestrador.ideogram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Acompanha o uso do plano gratuito do Ideogram (25 imagens/dia). Diferente do Gemini/Mistral, não aplica
 * fator de segurança sobre o limite: o Flux (Pollinations) já é o fallback automático quando o Ideogram
 * esgota o dia, então não há risco de custo real ao usar a cota inteira.
 *
 * O uso diário é persistido no banco (tabela ideogram_uso_diario) para sobreviver a restart.
 */
@Component
public class IdeogramLimiter {

    private static final Logger log = LoggerFactory.getLogger(IdeogramLimiter.class);

    private final int limiteImagensPorDia;
    private final IdeogramUsoDiarioRepository usoDiarioRepository;

    private IdeogramUsoDiario usoAtual;

    public IdeogramLimiter(IdeogramProperties properties, IdeogramUsoDiarioRepository usoDiarioRepository) {
        this.limiteImagensPorDia = Math.max(1, properties.getLimiteImagensPorDia());
        this.usoDiarioRepository = usoDiarioRepository;
        this.usoAtual = carregarOuCriar(LocalDate.now(ZoneId.systemDefault()));
        log.info("Limite diário do Ideogram: {} imagens. Uso já registrado hoje: {} imagens.",
                limiteImagensPorDia, usoAtual.getImagens());
    }

    private IdeogramUsoDiario carregarOuCriar(LocalDate dia) {
        return usoDiarioRepository.findById(dia).orElseGet(() -> usoDiarioRepository.save(new IdeogramUsoDiario(dia)));
    }

    /**
     * Lança LimiteIdeogramAtingidoException se a cota diária gratuita já foi atingida.
     */
    public synchronized void reservarRequisicao() {
        renovarDiaSeNecessario();
        if (usoAtual.getImagens() >= limiteImagensPorDia) {
            throw new LimiteIdeogramAtingidoException(
                    "Limite diário gratuito do Ideogram atingido (%d imagens/dia). Tente novamente amanhã."
                            .formatted(limiteImagensPorDia));
        }
        usoAtual.incrementarImagens();
        usoDiarioRepository.save(usoAtual);
    }

    /**
     * Chamado quando a própria API do Ideogram recusa a chamada por limite real da conta.
     * Trava o contador local no teto do dia para não tentar de novo até o dia virar.
     */
    public synchronized void registrarLimiteRealAtingido() {
        renovarDiaSeNecessario();
        usoAtual.saturar(limiteImagensPorDia);
        usoDiarioRepository.save(usoAtual);
    }

    public synchronized IdeogramUsoStatus statusAtual() {
        renovarDiaSeNecessario();
        return new IdeogramUsoStatus(usoAtual.getImagens(), limiteImagensPorDia);
    }

    private void renovarDiaSeNecessario() {
        LocalDate hoje = LocalDate.now(ZoneId.systemDefault());
        if (!hoje.equals(usoAtual.getDia())) {
            usoAtual = carregarOuCriar(hoje);
        }
    }
}
