package com.huber.orquestrador.configuracao;

public enum CriterioBusca {
    NOVIDADES("Novidades"),
    NOSTALGIA("Nostalgia"),
    TEORIAS("Teorias"),
    FERRAMENTAS("Ferramentas"),
    TECNICAS("Técnicas");

    private final String rotulo;

    CriterioBusca(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
