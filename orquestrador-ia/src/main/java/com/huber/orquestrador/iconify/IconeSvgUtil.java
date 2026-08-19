package com.huber.orquestrador.iconify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepara o SVG bruto devolvido pelo Iconify para ser embutido dentro de um template maior:
 * extrai o conteúdo interno, aplica a cor escolhida no lugar de "currentColor" e gera um <g>
 * posicionado/escalado para caber num quadrado de tamanho fixo, centrado em (cx, cy).
 */
public final class IconeSvgUtil {

    private static final Pattern VIEW_BOX = Pattern.compile("viewBox=\"[^\"]*?\\s([0-9.]+)\\s+([0-9.]+)\"");
    private static final Pattern CONTEUDO_INTERNO = Pattern.compile("<svg[^>]*>(.*)</svg>", Pattern.DOTALL);

    private IconeSvgUtil() {
    }

    public static String posicionar(String svgBruto, double cx, double cy, double tamanho, String cor) {
        double largura = 24;
        double altura = 24;
        Matcher viewBox = VIEW_BOX.matcher(svgBruto);
        if (viewBox.find()) {
            largura = Double.parseDouble(viewBox.group(1));
            altura = Double.parseDouble(viewBox.group(2));
        }

        Matcher conteudo = CONTEUDO_INTERNO.matcher(svgBruto);
        String interno = conteudo.find() ? conteudo.group(1) : svgBruto;
        interno = interno.replace("currentColor", cor);

        double escala = tamanho / Math.max(largura, altura);
        double x = cx - (largura * escala) / 2;
        double y = cy - (altura * escala) / 2;

        return "<g transform=\"translate(%s,%s) scale(%s)\">%s</g>"
                .formatted(fmt(x), fmt(y), fmt(escala), interno);
    }

    private static String fmt(double valor) {
        return String.valueOf(Math.round(valor * 1000) / 1000.0);
    }
}
