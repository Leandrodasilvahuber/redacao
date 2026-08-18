package com.huber.orquestrador.configuracao;

public enum EstiloIlustracao {
    ATUAL(
            "Estilo atual",
            "ilustração editorial desenhada à mão em estilo vetorial moderno, como as capas de blog de tecnologia"
    ),
    BITS_8(
            "8 bits",
            "estilo pixel art de 8 bits, paleta bem reduzida e blocos de pixel grandes e visíveis, "
                    + "como jogos de Atari/NES"
    ),
    BITS_16(
            "16 bits",
            "estilo pixel art de 16 bits, cores limitadas e blocos de pixel visíveis, como jogos de SNES/Genesis"
    ),
    BITS_32(
            "32 bits",
            "estilo pixel art de 32 bits, mais detalhado e com mais cores que 16 bits, "
                    + "como jogos de PlayStation 1/Saturn"
    ),
    CARTOON(
            "Desenho cartoon",
            "estilo cartoon, traços arredondados, cores vivas e chapadas, expressões exageradas, "
                    + "como desenho animado"
    );

    private final String rotulo;
    private final String descricaoPrompt;

    EstiloIlustracao(String rotulo, String descricaoPrompt) {
        this.rotulo = rotulo;
        this.descricaoPrompt = descricaoPrompt;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricaoPrompt() {
        return descricaoPrompt;
    }
}
