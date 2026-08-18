package com.huber.orquestrador.blog;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.noticia.Noticia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class BlogPublicadorService {

    private static final Logger log = LoggerFactory.getLogger(BlogPublicadorService.class);
    private static final int TAMANHO_MAXIMO_LEAD = 255;

    private final BlogClient blogClient;
    private final ConfiguracaoService configuracaoService;

    public BlogPublicadorService(BlogClient blogClient, ConfiguracaoService configuracaoService) {
        this.blogClient = blogClient;
        this.configuracaoService = configuracaoService;
    }

    public void publicar(Noticia noticia, byte[] imagemPng) {
        try {
            String lead = noticia.getTitulo().length() > TAMANHO_MAXIMO_LEAD
                    ? noticia.getTitulo().substring(0, TAMANHO_MAXIMO_LEAD)
                    : noticia.getTitulo();
            String coverImageBase64 = imagemPng != null && imagemPng.length > 0
                    ? Base64.getEncoder().encodeToString(imagemPng)
                    : null;

            BlogClient.PostRequest post = new BlogClient.PostRequest(
                    "Orquestrador de IA",
                    configuracaoService.getBlogIlustracaoPadrao().getValorNoBlog(),
                    lead,
                    noticia.getTextoFinal(),
                    "",
                    coverImageBase64,
                    noticia.getLink(),
                    0,
                    0,
                    0,
                    null,
                    null,
                    null,
                    null,
                    DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
            );

            String postId = blogClient.criarPost(post);
            noticia.marcarPublicadaNoBlog(postId);
        } catch (Exception e) {
            log.warn("Falha ao publicar notícia {} no blog: {}", noticia.getId(), e.getMessage());
            noticia.marcarErroBlog(e.getMessage());
        }
    }

    public void excluir(Noticia noticia) {
        if (noticia.getBlogPostId() == null || noticia.getBlogPostId().isBlank()) {
            return;
        }
        try {
            blogClient.excluirPost(noticia.getBlogPostId());
        } catch (Exception e) {
            log.warn("Falha ao excluir do blog o post {} da notícia {}: {}",
                    noticia.getBlogPostId(), noticia.getId(), e.getMessage());
        }
    }
}
