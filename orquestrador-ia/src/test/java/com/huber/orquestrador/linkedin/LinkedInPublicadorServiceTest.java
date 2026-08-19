package com.huber.orquestrador.linkedin;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.noticia.Noticia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre principalmente {@link LinkedInPublicadorService#republicarComNovaImagem}: a API do
 * LinkedIn não permite editar a imagem de um post existente, então trocar a capa exige publicar
 * de novo e excluir o post antigo. A ordem importa — publicar primeiro, excluir depois — pra que
 * uma falha na publicação nunca deixe a notícia sem nenhum post no ar.
 */
@ExtendWith(MockitoExtension.class)
class LinkedInPublicadorServiceTest {

    private static final String ACCESS_TOKEN = "token-123";
    private static final String PERSON_URN = "urn:li:person:abc";

    @Mock
    private LinkedInClient linkedInClient;

    @Mock
    private ConfiguracaoService configuracaoService;

    private LinkedInPublicadorService service;

    @BeforeEach
    void setUp() {
        service = new LinkedInPublicadorService(linkedInClient, configuracaoService);
    }

    private void conectado() {
        when(configuracaoService.getLinkedinAccessToken()).thenReturn(ACCESS_TOKEN);
        when(configuracaoService.getLinkedinPersonUrn()).thenReturn(PERSON_URN);
        when(configuracaoService.getLinkedinTokenExpiraEm()).thenReturn(Instant.now().plusSeconds(3600));
    }

    private Noticia noticiaComPostAntigo() {
        Noticia noticia = new Noticia("Título", "https://exemplo.com/1", "fonte", "resumo", Instant.now());
        noticia.marcarPublicadaNoLinkedin("urn:li:share:antigo");
        return noticia;
    }

    @Test
    void publicaOPostNovoAntesDeExcluirOAntigo() {
        conectado();
        Noticia noticia = noticiaComPostAntigo();
        when(linkedInClient.publicarPost(eq(ACCESS_TOKEN), eq(PERSON_URN), any(), any()))
                .thenReturn("urn:li:share:novo");

        service.republicarComNovaImagem(noticia, new byte[]{1, 2, 3});

        InOrder ordem = inOrder(linkedInClient);
        ordem.verify(linkedInClient).publicarPost(eq(ACCESS_TOKEN), eq(PERSON_URN), any(), any());
        ordem.verify(linkedInClient).excluirPost(ACCESS_TOKEN, "urn:li:share:antigo");
        assertThat(noticia.getLinkedinPostUrn()).isEqualTo("urn:li:share:novo");
    }

    @Test
    void seAPublicacaoNovaFalharNaoExcluiOPostAntigoENaoPerdeANoticia() {
        conectado();
        Noticia noticia = noticiaComPostAntigo();
        when(linkedInClient.publicarPost(anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("LinkedIn fora do ar"));

        assertThatThrownBy(() -> service.republicarComNovaImagem(noticia, new byte[]{1}))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("LinkedIn fora do ar");

        verify(linkedInClient, never()).excluirPost(anyString(), anyString());
        // a notícia continua "publicada" com a URN antiga, nunca fica sem nenhum post
        assertThat(noticia.getLinkedinPostUrn()).isEqualTo("urn:li:share:antigo");
    }

    @Test
    void seExcluirOPostAntigoFalharDepoisDoNovoPublicadoNaoPropagaAExcecao() {
        conectado();
        Noticia noticia = noticiaComPostAntigo();
        when(linkedInClient.publicarPost(eq(ACCESS_TOKEN), eq(PERSON_URN), any(), any()))
                .thenReturn("urn:li:share:novo");
        org.mockito.Mockito.doThrow(new RuntimeException("falha ao excluir"))
                .when(linkedInClient).excluirPost(ACCESS_TOKEN, "urn:li:share:antigo");

        service.republicarComNovaImagem(noticia, new byte[]{1});

        // o post novo já foi publicado e a notícia já reflete isso, mesmo com a exclusão falhando
        assertThat(noticia.getLinkedinPostUrn()).isEqualTo("urn:li:share:novo");
    }

    @Test
    void naoTentaExcluirNadaQuandoNaoHaviaPostAnterior() {
        conectado();
        Noticia noticia = new Noticia("Título", "https://exemplo.com/2", "fonte", "resumo", Instant.now());
        when(linkedInClient.publicarPost(eq(ACCESS_TOKEN), eq(PERSON_URN), any(), any()))
                .thenReturn("urn:li:share:primeiro");

        service.republicarComNovaImagem(noticia, new byte[]{1});

        verify(linkedInClient, never()).excluirPost(anyString(), anyString());
        assertThat(noticia.getLinkedinPostUrn()).isEqualTo("urn:li:share:primeiro");
    }

    @Test
    void naoTentaPublicarQuandoLinkedinNaoEstaConectado() {
        when(configuracaoService.getLinkedinAccessToken()).thenReturn("");
        Noticia noticia = noticiaComPostAntigo();

        assertThatThrownBy(() -> service.republicarComNovaImagem(noticia, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não está conectado");

        verify(linkedInClient, never()).publicarPost(anyString(), anyString(), any(), any());
        verify(linkedInClient, never()).excluirPost(anyString(), anyString());
    }

    @Test
    void naoTentaPublicarQuandoOTokenExpirou() {
        when(configuracaoService.getLinkedinAccessToken()).thenReturn(ACCESS_TOKEN);
        when(configuracaoService.getLinkedinPersonUrn()).thenReturn(PERSON_URN);
        when(configuracaoService.getLinkedinTokenExpiraEm()).thenReturn(Instant.now().minusSeconds(1));
        Noticia noticia = noticiaComPostAntigo();

        assertThatThrownBy(() -> service.republicarComNovaImagem(noticia, new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expirou");

        verify(linkedInClient, never()).publicarPost(anyString(), anyString(), any(), any());
    }

    @Test
    void publicarNuncaLancaExcecaoEMarcaOErroNaNoticia() {
        conectado();
        Noticia noticia = new Noticia("Título", "https://exemplo.com/3", "fonte", "resumo", Instant.now());
        when(linkedInClient.publicarPost(anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("falha de rede"));

        service.publicar(noticia, new byte[]{1});

        assertThat(noticia.getLinkedinErro()).isEqualTo("falha de rede");
        assertThat(noticia.getLinkedinPostUrn()).isNull();
    }
}
