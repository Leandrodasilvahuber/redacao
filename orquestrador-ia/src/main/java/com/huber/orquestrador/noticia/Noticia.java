package com.huber.orquestrador.noticia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "noticias")
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, unique = true, length = 1000)
    private String link;

    @Column(nullable = false)
    private String fonte;

    @Lob
    private String resumoOriginal;

    private Instant dataPublicacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNoticia estado;

    @Lob
    private String textoRedigido;

    @Lob
    private String textoRevisado;

    @Lob
    private String textoFinal;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Noticia() {
    }

    public Noticia(String titulo, String link, String fonte, String resumoOriginal, Instant dataPublicacao) {
        this.titulo = titulo;
        this.link = link;
        this.fonte = fonte;
        this.resumoOriginal = resumoOriginal;
        this.dataPublicacao = dataPublicacao;
        this.estado = EstadoNoticia.BUSCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public void mudarEstado(EstadoNoticia novoEstado) {
        this.estado = novoEstado;
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getLink() {
        return link;
    }

    public String getFonte() {
        return fonte;
    }

    public String getResumoOriginal() {
        return resumoOriginal;
    }

    public Instant getDataPublicacao() {
        return dataPublicacao;
    }

    public EstadoNoticia getEstado() {
        return estado;
    }

    public String getTextoRedigido() {
        return textoRedigido;
    }

    public void setTextoRedigido(String textoRedigido) {
        this.textoRedigido = textoRedigido;
        this.atualizadoEm = Instant.now();
    }

    public String getTextoRevisado() {
        return textoRevisado;
    }

    public void setTextoRevisado(String textoRevisado) {
        this.textoRevisado = textoRevisado;
        this.atualizadoEm = Instant.now();
    }

    public String getTextoFinal() {
        return textoFinal;
    }

    public void setTextoFinal(String textoFinal) {
        this.textoFinal = textoFinal;
        this.atualizadoEm = Instant.now();
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
