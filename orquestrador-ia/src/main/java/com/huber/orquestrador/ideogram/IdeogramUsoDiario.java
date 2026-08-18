package com.huber.orquestrador.ideogram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "ideogram_uso_diario")
public class IdeogramUsoDiario {

    @Id
    private LocalDate dia;

    @Column(nullable = false)
    private int imagens;

    protected IdeogramUsoDiario() {
    }

    public IdeogramUsoDiario(LocalDate dia) {
        this.dia = dia;
        this.imagens = 0;
    }

    public LocalDate getDia() {
        return dia;
    }

    public int getImagens() {
        return imagens;
    }

    public void incrementarImagens() {
        this.imagens++;
    }

    public void saturar(int limite) {
        this.imagens = Math.max(this.imagens, limite);
    }
}
