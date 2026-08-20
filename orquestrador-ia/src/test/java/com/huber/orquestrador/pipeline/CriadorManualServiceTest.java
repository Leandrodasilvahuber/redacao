package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.mistral.MistralClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriadorManualServiceTest {

    @Mock
    private MistralClient mistralClient;

    @Mock
    private NoticiaRepository noticiaRepository;

    private CriadorManualService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JsonMapper();
        service = new CriadorManualService(mistralClient, noticiaRepository, objectMapper);
    }

    @Test
    void formatarComoNoticiaDevolveTituloETextoDoMistral() {
        when(mistralClient.chat(anyString(), eq("texto colado pelo usuário"), eq(true)))
                .thenReturn("{\"titulo\":\"IA anuncia novidade\",\"texto\":\"Corpo formatado da notícia.\"}");

        CriadorManualService.FormatoGerado gerado =
                service.formatar("texto colado pelo usuário", CriadorManualService.TipoFormatacao.NOTICIA);

        assertThat(gerado.titulo()).isEqualTo("IA anuncia novidade");
        assertThat(gerado.texto()).isEqualTo("Corpo formatado da notícia.");
    }

    @Test
    void formatarComoTutorialUsaUmPromptDiferenteDoDeNoticia() {
        ArgumentCaptor<String> promptNoticiaCapturado = ArgumentCaptor.forClass(String.class);
        when(mistralClient.chat(anyString(), eq("passo a passo colado"), eq(true)))
                .thenReturn("{\"titulo\":\"Como fazer X\",\"texto\":\"1. Primeiro passo.\"}");

        service.formatar("passo a passo colado", CriadorManualService.TipoFormatacao.TUTORIAL);

        verify(mistralClient).chat(promptNoticiaCapturado.capture(), eq("passo a passo colado"), eq(true));
        assertThat(promptNoticiaCapturado.getValue()).contains("tutorial");
    }

    @Test
    void formatarLancaExcecaoQuandoIaNaoDevolveTituloOuTexto() {
        when(mistralClient.chat(anyString(), anyString(), eq(true)))
                .thenReturn("{\"titulo\":\"\",\"texto\":\"\"}");

        assertThatThrownBy(() -> service.formatar("texto qualquer", CriadorManualService.TipoFormatacao.NOTICIA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void formatarPropagaLimiteMistralAtingido() {
        when(mistralClient.chat(anyString(), anyString(), eq(true)))
                .thenThrow(new LimiteMistralAtingidoException("orçamento estourado"));

        assertThatThrownBy(() -> service.formatar("texto qualquer", CriadorManualService.TipoFormatacao.NOTICIA))
                .isInstanceOf(LimiteMistralAtingidoException.class);
    }

    @Test
    void salvarCriaNoticiaNoEstadoRevisadaComLinkSinteticoUnico() {
        when(noticiaRepository.save(org.mockito.ArgumentMatchers.any(Noticia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Noticia noticia = service.salvar("Título manual", "Texto já formatado");

        assertThat(noticia.getTitulo()).isEqualTo("Título manual");
        assertThat(noticia.getTextoRevisado()).isEqualTo("Texto já formatado");
        assertThat(noticia.getEstado()).isEqualTo(EstadoNoticia.REVISADA);
        assertThat(noticia.getFonte()).isEqualTo("Manual");
        assertThat(noticia.getLink()).startsWith("manual:");
        verify(noticiaRepository).save(noticia);
    }
}
