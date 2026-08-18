package com.huber.orquestrador.configuracao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "configuracoes")
public class Configuracao {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id;

    private String groqApiKey;

    private String geminiApiKey;

    private String mistralApiKey;

    @Column(nullable = false)
    private String criteriosBusca;

    @Column(nullable = false)
    private boolean revisarFonteVeridica;

    @Column(nullable = false)
    private boolean revisarEstrutura;

    @Column(nullable = false)
    private boolean revisarPadraoLinkedin;

    @Column(nullable = false)
    private boolean atribuirFonte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstiloIlustracao estiloIlustracao;

    private String blogApiUrl;

    private String blogApiToken;

    @Enumerated(EnumType.STRING)
    private BlogIlustracao blogIlustracaoPadrao;

    private String linkedinClientId;

    private String linkedinClientSecret;

    @Column(length = 2000)
    private String linkedinAccessToken;

    private Instant linkedinTokenExpiraEm;

    private String linkedinPersonUrn;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Configuracao() {
    }

    public Configuracao(Long id) {
        this.id = id;
        this.criteriosBusca = "";
        this.revisarFonteVeridica = true;
        this.revisarEstrutura = true;
        this.revisarPadraoLinkedin = true;
        this.atribuirFonte = false;
        this.estiloIlustracao = EstiloIlustracao.ATUAL;
        this.blogApiUrl = "https://leandrohuber.duckdns.org";
        this.blogIlustracaoPadrao = BlogIlustracao.TERMINAL;
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getGroqApiKey() {
        return groqApiKey;
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getMistralApiKey() {
        return mistralApiKey;
    }

    public void setMistralApiKey(String mistralApiKey) {
        this.mistralApiKey = mistralApiKey;
    }

    public String getCriteriosBusca() {
        return criteriosBusca;
    }

    public void setCriteriosBusca(String criteriosBusca) {
        this.criteriosBusca = criteriosBusca;
    }

    public boolean isRevisarFonteVeridica() {
        return revisarFonteVeridica;
    }

    public void setRevisarFonteVeridica(boolean revisarFonteVeridica) {
        this.revisarFonteVeridica = revisarFonteVeridica;
    }

    public boolean isRevisarEstrutura() {
        return revisarEstrutura;
    }

    public void setRevisarEstrutura(boolean revisarEstrutura) {
        this.revisarEstrutura = revisarEstrutura;
    }

    public boolean isRevisarPadraoLinkedin() {
        return revisarPadraoLinkedin;
    }

    public void setRevisarPadraoLinkedin(boolean revisarPadraoLinkedin) {
        this.revisarPadraoLinkedin = revisarPadraoLinkedin;
    }

    public boolean isAtribuirFonte() {
        return atribuirFonte;
    }

    public void setAtribuirFonte(boolean atribuirFonte) {
        this.atribuirFonte = atribuirFonte;
    }

    public EstiloIlustracao getEstiloIlustracao() {
        return estiloIlustracao;
    }

    public void setEstiloIlustracao(EstiloIlustracao estiloIlustracao) {
        this.estiloIlustracao = estiloIlustracao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getBlogApiUrl() {
        return blogApiUrl;
    }

    public void setBlogApiUrl(String blogApiUrl) {
        this.blogApiUrl = blogApiUrl;
    }

    public String getBlogApiToken() {
        return blogApiToken;
    }

    public void setBlogApiToken(String blogApiToken) {
        this.blogApiToken = blogApiToken;
    }

    public BlogIlustracao getBlogIlustracaoPadrao() {
        return blogIlustracaoPadrao;
    }

    public void setBlogIlustracaoPadrao(BlogIlustracao blogIlustracaoPadrao) {
        this.blogIlustracaoPadrao = blogIlustracaoPadrao;
    }

    public String getLinkedinClientId() {
        return linkedinClientId;
    }

    public void setLinkedinClientId(String linkedinClientId) {
        this.linkedinClientId = linkedinClientId;
    }

    public String getLinkedinClientSecret() {
        return linkedinClientSecret;
    }

    public void setLinkedinClientSecret(String linkedinClientSecret) {
        this.linkedinClientSecret = linkedinClientSecret;
    }

    public String getLinkedinAccessToken() {
        return linkedinAccessToken;
    }

    public void setLinkedinAccessToken(String linkedinAccessToken) {
        this.linkedinAccessToken = linkedinAccessToken;
    }

    public Instant getLinkedinTokenExpiraEm() {
        return linkedinTokenExpiraEm;
    }

    public void setLinkedinTokenExpiraEm(Instant linkedinTokenExpiraEm) {
        this.linkedinTokenExpiraEm = linkedinTokenExpiraEm;
    }

    public String getLinkedinPersonUrn() {
        return linkedinPersonUrn;
    }

    public void setLinkedinPersonUrn(String linkedinPersonUrn) {
        this.linkedinPersonUrn = linkedinPersonUrn;
    }

    public void marcarAtualizada() {
        this.atualizadoEm = Instant.now();
    }
}
