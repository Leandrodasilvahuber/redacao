package com.huber.orquestrador.mistral;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "mistral_uso_diario")
public class MistralUsoDiario {

    @Id
    private LocalDate dia;

    @Column(nullable = false)
    private int requisicoes;

    @Column(nullable = false)
    private long tokensEntrada;

    @Column(nullable = false)
    private long tokensSaida;

    protected MistralUsoDiario() {
    }

    public MistralUsoDiario(LocalDate dia) {
        this.dia = dia;
        this.requisicoes = 0;
        this.tokensEntrada = 0;
        this.tokensSaida = 0;
    }

    public LocalDate getDia() {
        return dia;
    }

    public int getRequisicoes() {
        return requisicoes;
    }

    public long getTokensEntrada() {
        return tokensEntrada;
    }

    public long getTokensSaida() {
        return tokensSaida;
    }

    public void incrementarRequisicoes() {
        this.requisicoes++;
    }

    public void somarTokens(long entrada, long saida) {
        this.tokensEntrada += entrada;
        this.tokensSaida += saida;
    }
}
