package com.huber.orquestrador.groq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "groq_uso_diario")
public class GroqUsoDiario {

    @Id
    private LocalDate dia;

    @Column(nullable = false)
    private int requisicoes;

    @Column(nullable = false)
    private int tokens;

    protected GroqUsoDiario() {
    }

    public GroqUsoDiario(LocalDate dia) {
        this.dia = dia;
        this.requisicoes = 0;
        this.tokens = 0;
    }

    public LocalDate getDia() {
        return dia;
    }

    public int getRequisicoes() {
        return requisicoes;
    }

    public int getTokens() {
        return tokens;
    }

    public void incrementarRequisicoes() {
        this.requisicoes++;
    }

    public void somarTokens(int quantidade) {
        this.tokens += quantidade;
    }
}
