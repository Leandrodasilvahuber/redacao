package com.huber.orquestrador.blog;

import com.huber.orquestrador.noticia.Noticia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlogPublicadorServiceTest {

    @Mock
    private BlogClient blogClient;

    private BlogPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new BlogPublicadorService(blogClient);
    }

    private Noticia noticiaPublicadaNoBlog() {
        Noticia noticia = new Noticia("Título", "https://exemplo.com/1", "fonte", "resumo", Instant.now());
        noticia.marcarPublicadaNoBlog("42");
        return noticia;
    }

    @Test
    void reenviaACapaCodificadaEmBase64ProPostExistente() {
        Noticia noticia = noticiaPublicadaNoBlog();
        byte[] imagem = {10, 20, 30};

        service.atualizarCapa(noticia, imagem);

        ArgumentCaptor<String> base64Captor = ArgumentCaptor.forClass(String.class);
        verify(blogClient).atualizarCapa(eq("42"), base64Captor.capture());
        assertThat(Base64.getDecoder().decode(base64Captor.getValue())).isEqualTo(imagem);
    }

    @Test
    void naoTentaAtualizarQuandoANoticiaNaoTemPostNoBlog() {
        Noticia noticia = new Noticia("Título", "https://exemplo.com/2", "fonte", "resumo", Instant.now());

        assertThatThrownBy(() -> service.atualizarCapa(noticia, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não tem post no blog");

        verify(blogClient, never()).atualizarCapa(anyString(), anyString());
    }

    @Test
    void rejeitaImagemNulaAntesDeChamarOBlog() {
        Noticia noticia = noticiaPublicadaNoBlog();

        assertThatThrownBy(() -> service.atualizarCapa(noticia, null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(blogClient, never()).atualizarCapa(anyString(), anyString());
    }

    @Test
    void rejeitaImagemVaziaAntesDeChamarOBlog() {
        Noticia noticia = noticiaPublicadaNoBlog();

        assertThatThrownBy(() -> service.atualizarCapa(noticia, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);

        verify(blogClient, never()).atualizarCapa(anyString(), anyString());
    }
}
