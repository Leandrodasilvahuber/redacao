package com.huber.orquestrador.configuracao;

/**
 * Biblioteca de ícones (coleção do Iconify) usada para restringir a busca do ícone da capa ao
 * estilo escolhido pelo usuário, em vez de misturar ícones de qualquer uma das 200+ coleções do
 * Iconify.
 */
public enum BibliotecaIcones {
    MATERIAL_SYMBOLS("material-symbols"),
    MATERIAL_ICONS("ic"),
    TABLER("tabler"),
    PHOSPHOR("ph");

    private final String prefixoIconify;

    BibliotecaIcones(String prefixoIconify) {
        this.prefixoIconify = prefixoIconify;
    }

    public String getPrefixoIconify() {
        return prefixoIconify;
    }
}
