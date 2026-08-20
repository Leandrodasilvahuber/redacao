package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeletorServiceTest {

    @Mock
    private NoticiaRepository noticiaRepository;

    @Mock
    private ClienteTextoIa clienteTextoIa;

    @Mock
    private ConfiguracaoService configuracaoService;

    private SeletorService service;

    @BeforeEach
    void setUp() {
        service = new SeletorService(noticiaRepository, clienteTextoIa, configuracaoService, 10);
        when(configuracaoService.getCriteriosBuscaAtivos()).thenReturn(List.of());
    }

    private Noticia noticiaBuscada() {
        Noticia noticia = new Noticia("Python 3.13 traz novidades", "https://exemplo.com/1", "fonte", "resumo", Instant.now());
        return noticia;
    }

    @Test
    void semTermoNaoAdicionaPrioridadeExtraAoPrompt() {
        Noticia noticia = noticiaBuscada();
        when(noticiaRepository.findByEstado(EstadoNoticia.BUSCADA)).thenReturn(List.of(noticia));
        when(noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA)).thenReturn(List.of());
        when(clienteTextoIa.chat(any(), anyString(), anyString())).thenReturn("SIM");

        service.selecionar(null, null);

        ArgumentCaptor<String> promptCapturado = ArgumentCaptor.forClass(String.class);
        verify(clienteTextoIa).chat(any(), promptCapturado.capture(), anyString());
        assertThat(promptCapturado.getValue()).doesNotContain("Prioridade adicional pedida pelo usuário");
    }

    @Test
    void comTermoAdicionaPrioridadeExtraAoPrompt() {
        Noticia noticia = noticiaBuscada();
        when(noticiaRepository.findByEstado(EstadoNoticia.BUSCADA)).thenReturn(List.of(noticia));
        when(noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA)).thenReturn(List.of());
        when(clienteTextoIa.chat(any(), anyString(), anyString())).thenReturn("SIM");

        service.selecionar(null, "carros elétricos");

        ArgumentCaptor<String> promptCapturado = ArgumentCaptor.forClass(String.class);
        verify(clienteTextoIa).chat(any(), promptCapturado.capture(), anyString());
        assertThat(promptCapturado.getValue()).contains("Prioridade adicional pedida pelo usuário nesta busca: \"carros elétricos\"");
    }

    @Test
    void selecionarComIdSemTermoMantemComportamentoAnterior() {
        Noticia noticia = noticiaBuscada();
        when(noticiaRepository.findById(1L)).thenReturn(java.util.Optional.of(noticia));
        when(noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA)).thenReturn(List.of());
        when(clienteTextoIa.chat(any(), anyString(), anyString())).thenReturn("SIM");

        int selecionadas = service.selecionar(1L);

        assertThat(selecionadas).isEqualTo(1);
        assertThat(noticia.getEstado()).isEqualTo(EstadoNoticia.SELECIONADA);
    }
}
