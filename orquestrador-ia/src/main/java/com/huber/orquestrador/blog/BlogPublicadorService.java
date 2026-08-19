package com.huber.orquestrador.blog;

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

    /**
     * Valor fixo enviado no campo "illustration" da API do blog (metadado exigido pelo schema
     * externo). A capa de fato é a imagem real gerada em {@code coverImageBase64}, com o ícone
     * escolhido pela IA de acordo com o tema da notícia — este campo não influencia o visual.
     */
    private static final String ILUSTRACAO_FIXA = "terminal";

    private final BlogClient blogClient;

    public BlogPublicadorService(BlogClient blogClient) {
        this.blogClient = blogClient;
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
                    ILUSTRACAO_FIXA,
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

    /**
     * Reenvia a capa de um post já publicado (usado pra atualizar posts antigos pro padrão visual
     * atual). Ao contrário de {@link #publicar}, deixa a exceção subir: aqui é uma ação manual e
     * pontual, então o chamador precisa saber na hora se falhou, em vez de só logar.
     */
    public void atualizarCapa(Noticia noticia, byte[] imagemPng) {
        if (noticia.getBlogPostId() == null || noticia.getBlogPostId().isBlank()) {
            throw new IllegalStateException("Notícia " + noticia.getId() + " não tem post no blog");
        }
        String coverImageBase64 = Base64.getEncoder().encodeToString(imagemPng);
        blogClient.atualizarCapa(noticia.getBlogPostId(), coverImageBase64);
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
