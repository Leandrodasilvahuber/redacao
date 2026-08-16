package com.huber.orquestrador.pipeline;

final class Pausa {

    private static final long MILISSEGUNDOS_ENTRE_CHAMADAS = 1500;

    private Pausa() {
    }

    static void aguardar() {
        try {
            Thread.sleep(MILISSEGUNDOS_ENTRE_CHAMADAS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
