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
    private static final Pattern STROKE_WIDTH = Pattern.compile("stroke-width=\"[0-9.]+\"");

    /**
     * Espessura de traço alvo já em escala do canvas final (1200x627), pra ícones de traço (Tabler,
     * Phosphor outline etc.) ficarem com o mesmo peso visual dos ícones de linha desenhados à mão do
     * blog, em vez de engrossar proporcionalmente ao tamanho do ícone.
     */
    private static final double ESPESSURA_TRACO_ALVO = 12;

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

        if (interno.contains("stroke-width=")) {
            String espessuraFixa = fmt(ESPESSURA_TRACO_ALVO / escala);
            interno = STROKE_WIDTH.matcher(interno).replaceAll("stroke-width=\"" + espessuraFixa + "\"");
        }

        double x = cx - (largura * escala) / 2;
        double y = cy - (altura * escala) / 2;

        return "<g transform=\"translate(%s,%s) scale(%s)\">%s</g>"
                .formatted(fmt(x), fmt(y), fmt(escala), interno);
    }

    private static String fmt(double valor) {
        return String.valueOf(Math.round(valor * 1000) / 1000.0);
    }
}
