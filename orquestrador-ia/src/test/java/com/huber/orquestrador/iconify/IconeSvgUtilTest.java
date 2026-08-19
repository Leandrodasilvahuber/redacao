package com.huber.orquestrador.iconify;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IconeSvgUtilTest {

    private static final String ICONE_TABLER =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" d=\"M4 4h16v16H4z\"/></svg>";

    private static final String ICONE_PREENCHIDO =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"currentColor\" d=\"M4 4h16v16H4z\"/></svg>";

    @Test
    void substituiCurrentColorPelaCorEscolhida() {
        String resultado = IconeSvgUtil.posicionar(ICONE_PREENCHIDO, 600, 313, 420, "#8CF7FF");

        assertThat(resultado).contains("fill=\"#8CF7FF\"");
        assertThat(resultado).doesNotContain("currentColor");
    }

    @Test
    void centralizaOIconeNoPontoInformadoConsiderandoOTamanho() {
        // viewBox 24x24, tamanho 240 -> escala 10; centralizado em (600,313) fica em
        // x = 600 - (24*10)/2 = 480, y = 313 - (24*10)/2 = 193
        String resultado = IconeSvgUtil.posicionar(ICONE_PREENCHIDO, 600, 313, 240, "#000000");

        assertThat(resultado).contains("translate(480.0,193.0)").contains("scale(10.0)");
    }

    @Test
    void normalizaAEspessuraDoTracoIndependenteDoTamanhoDoIcone() {
        // Sem normalização, um ícone de traço ficaria proporcionalmente mais grosso quanto maior o
        // ícone. A espessura final (alvo / escala) deve ser a mesma pro mesmo alvo, ajustada só pela
        // escala de cada chamada.
        String iconePequeno = IconeSvgUtil.posicionar(ICONE_TABLER, 600, 313, 240, "#8CF7FF");
        String iconeGrande = IconeSvgUtil.posicionar(ICONE_TABLER, 600, 313, 480, "#8CF7FF");

        // escala pequeno = 240/24 = 10 -> stroke-width = 12/10 = 1.2
        assertThat(iconePequeno).contains("stroke-width=\"1.2\"");
        // escala grande = 480/24 = 20 -> stroke-width = 12/20 = 0.6
        assertThat(iconeGrande).contains("stroke-width=\"0.6\"");
        assertThat(iconePequeno).doesNotContain("stroke-width=\"2\"");
    }

    @Test
    void naoMexeEmIconePreenchidoSemStrokeWidth() {
        String resultado = IconeSvgUtil.posicionar(ICONE_PREENCHIDO, 600, 313, 420, "#8CF7FF");

        assertThat(resultado).doesNotContain("stroke-width");
    }

    @Test
    void usaViewBox24x24ComoPadraoQuandoNaoInformado() {
        String semViewBox = "<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<path fill=\"currentColor\" d=\"M0 0h1v1H0z\"/></svg>";

        // tamanho 240, largura/altura padrão 24 -> escala 10, igual ao teste de centralização acima
        String resultado = IconeSvgUtil.posicionar(semViewBox, 600, 313, 240, "#fff");

        assertThat(resultado).contains("scale(10.0)");
    }
}
