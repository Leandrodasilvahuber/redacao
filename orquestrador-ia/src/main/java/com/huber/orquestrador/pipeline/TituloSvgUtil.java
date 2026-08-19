package com.huber.orquestrador.pipeline;

import java.util.ArrayList;
import java.util.List;

/** Quebra o título da notícia em linhas curtas e monta os &lt;tspan&gt; para um &lt;text&gt; de SVG. */
final class TituloSvgUtil {

    private TituloSvgUtil() {
    }

    static String tspans(String titulo, double x, double alturaLinha, int maxCaracteresPorLinha, int maxLinhas) {
        List<String> linhas = quebrar(titulo, maxCaracteresPorLinha, maxLinhas);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < linhas.size(); i++) {
            String dy = i == 0 ? "0" : String.valueOf(alturaLinha);
            sb.append("<tspan x=\"").append(fmt(x)).append("\" dy=\"").append(dy).append("\">")
                    .append(escapar(linhas.get(i))).append("</tspan>");
        }
        return sb.toString();
    }

    private static List<String> quebrar(String texto, int maxCaracteresPorLinha, int maxLinhas) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.trim().split("\\s+")) {
            if (atual.length() > 0 && atual.length() + 1 + palavra.length() > maxCaracteresPorLinha) {
                linhas.add(atual.toString());
                atual = new StringBuilder();
                if (linhas.size() == maxLinhas - 1) {
                    break;
                }
            }
            if (atual.length() > 0) {
                atual.append(' ');
            }
            atual.append(palavra);
        }
        if (atual.length() > 0) {
            linhas.add(atual.toString());
        }
        if (linhas.size() > maxLinhas) {
            linhas = linhas.subList(0, maxLinhas);
        }
        int ultimoIndice = linhas.size() - 1;
        String ultima = linhas.get(ultimoIndice);
        if (ultima.length() > maxCaracteresPorLinha) {
            ultima = ultima.substring(0, maxCaracteresPorLinha - 1).trim() + "…";
            linhas.set(ultimoIndice, ultima);
        } else if (linhas.size() == maxLinhas && !texto.trim().endsWith(ultima.trim())
                && !ultima.endsWith("…")) {
            linhas.set(ultimoIndice, ultima + "…");
        }
        return linhas;
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String fmt(double valor) {
        return String.valueOf(Math.round(valor * 1000) / 1000.0);
    }
}
