package com.huber.orquestrador.configuracao;

public enum ProvedorIlustracao {
    GEMINI("Gemini (desenho SVG)"),
    IDEOGRAM("Ideogram (imagem gerada, 25/dia grátis)"),
    FLUX("Flux Schnell (imagem gerada)");

    private final String rotulo;

    ProvedorIlustracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
