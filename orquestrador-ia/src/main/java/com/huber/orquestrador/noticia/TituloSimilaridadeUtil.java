package com.huber.orquestrador.noticia;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detecta títulos que descrevem a mesma notícia mesmo vindo de fontes/links diferentes (ex.: o
 * mesmo furo replicado por dois sites com manchetes reescritas). Compara os conjuntos de palavras
 * significativas dos dois títulos: quanto maior a sobreposição em relação ao menor título, mais
 * provável que seja a mesma notícia.
 */
public final class TituloSimilaridadeUtil {

    private static final double LIMIAR_SIMILARIDADE = 0.6;

    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9\\s]");

    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "da", "do", "das", "dos",
            "em", "no", "na", "nos", "nas", "por", "para", "com", "sem", "e", "ou", "que", "se",
            "é", "ao", "aos", "à", "às", "seu", "sua", "seus", "suas", "diz", "após",
            "sobre", "como", "mais", "menos", "novo", "nova", "novos", "novas"
    );

    private TituloSimilaridadeUtil() {
    }

    public static Set<String> normalizar(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return Set.of();
        }
        String semAcento = Normalizer.normalize(titulo.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpo = NAO_ALFANUMERICO.matcher(semAcento).replaceAll(" ");

        Set<String> palavras = new HashSet<>();
        for (String palavra : limpo.split("\\s+")) {
            if (palavra.length() >= 3 && !PALAVRAS_IGNORADAS.contains(palavra)) {
                palavras.add(palavra);
            }
        }
        return palavras;
    }

    /** Compara dois títulos já normalizados em conjuntos de palavras. */
    public static boolean saoDuplicados(Set<String> palavrasA, Set<String> palavrasB) {
        if (palavrasA.isEmpty() || palavrasB.isEmpty()) {
            return false;
        }
        Set<String> intersecao = new HashSet<>(palavrasA);
        intersecao.retainAll(palavrasB);

        int menor = Math.min(palavrasA.size(), palavrasB.size());
        return ((double) intersecao.size() / menor) >= LIMIAR_SIMILARIDADE;
    }

    public static boolean saoDuplicados(String tituloA, String tituloB) {
        return saoDuplicados(normalizar(tituloA), normalizar(tituloB));
    }
}
