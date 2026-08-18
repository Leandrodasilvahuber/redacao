package com.huber.orquestrador.configuracao;

public enum BlogIlustracao {
    BRAIN("brain"),
    CLOUD("cloud"),
    TERMINAL("terminal"),
    GRAPH("graph"),
    BRANCH("branch"),
    SHIELD("shield");

    private final String valorNoBlog;

    BlogIlustracao(String valorNoBlog) {
        this.valorNoBlog = valorNoBlog;
    }

    public String getValorNoBlog() {
        return valorNoBlog;
    }
}
