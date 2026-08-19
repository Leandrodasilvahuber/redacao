package com.huber.orquestrador.noticia;

import java.text.Normalizer;
import java.util.List;

/**
 * Filtro rápido (sem IA) para descartar de cara notícias que são na verdade publicidade/oferta de
 * produto (listas de "melhores preços", cupons de loja, etc.), muito comuns em feeds de tecnologia
 * misturados com conteúdo editorial. Só olha o título: o resumo/corpo da notícia costuma citar
 * marcas como Amazon ou Mercado Livre mesmo em matérias que não são anúncios, o que gerava falsos
 * positivos. Casos mais sutis (publieditorial, conteúdo patrocinado sem essas palavras-chave) ficam
 * por conta do prompt de seleção da IA.
 */
public final class PromocaoUtil {

    private static final List<String> PALAVRAS_CHAVE = List.of(
            "promocao", "promocoes", "cupom", "cupons", "desconto", "descontos", "oferta", "ofertas",
            "aliexpress", "mercado livre", "magalu", "shopee", "black friday", "por menos de r$"
    );

    private PromocaoUtil() {
    }

    public static boolean pareceAnuncioOuPromocao(String titulo) {
        String texto = normalizar(titulo != null ? titulo : "");
        return PALAVRAS_CHAVE.stream().anyMatch(texto::contains);
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento;
    }
}
