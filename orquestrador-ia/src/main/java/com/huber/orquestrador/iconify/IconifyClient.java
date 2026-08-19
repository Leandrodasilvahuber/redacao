package com.huber.orquestrador.iconify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente para o Iconify (api.iconify.design): serviço público, sem chave, que agrega mais de
 * 200 bibliotecas de ícones open source. Usado para localizar um ícone real ligado ao tema da
 * notícia em vez de desenhar uma ilustração do zero.
 */
@Component
public class IconifyClient {

    private static final Logger log = LoggerFactory.getLogger(IconifyClient.class);

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.iconify.design")
            .build();

    /**
     * Busca o ícone mais relevante para o termo e devolve o SVG completo (com viewBox original).
     * Lança exceção se nada for encontrado, para o chamador decidir o fallback.
     */
    public String buscarIconeSvg(String termoBusca) {
        SearchResponse resposta = restClient.get()
                .uri("/search?query={termo}&limit=1", termoBusca)
                .retrieve()
                .body(SearchResponse.class);

        if (resposta == null || resposta.icons() == null || resposta.icons().isEmpty()) {
            throw new IllegalStateException("Nenhum ícone encontrado no Iconify para: " + termoBusca);
        }

        String iconeCompleto = resposta.icons().get(0);
        int separador = iconeCompleto.indexOf(':');
        if (separador < 0) {
            throw new IllegalStateException("Formato de ícone inesperado do Iconify: " + iconeCompleto);
        }
        String prefixo = iconeCompleto.substring(0, separador);
        String nome = iconeCompleto.substring(separador + 1);

        log.info("Ícone escolhido pelo Iconify para \"{}\": {}", termoBusca, iconeCompleto);
        return restClient.get()
                .uri("/{prefixo}/{nome}.svg", prefixo, nome)
                .retrieve()
                .body(String.class);
    }

    private record SearchResponse(List<String> icons) {
    }
}
