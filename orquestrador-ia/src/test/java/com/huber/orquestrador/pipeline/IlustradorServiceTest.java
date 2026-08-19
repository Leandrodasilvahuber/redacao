package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.gemini.LimiteGeminiAtingidoException;
import com.huber.orquestrador.iconify.IconifyClient;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.mistral.MistralClient;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre {@link IlustradorService#regerarIlustracao}: a capa precisa reproduzir a identidade
 * visual do blog (fundo quadriculado, ícone sempre em #8CF7FF, cor de destaque dentro da paleta
 * fixa) e cair nos fallbacks certos quando o Iconify ou as IAs falham.
 */
@ExtendWith(MockitoExtension.class)
class IlustradorServiceTest {

    private static final String ICONE_TABLER =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" d=\"M4 4h16v16H4z\"/></svg>";

    @Mock
    private NoticiaRepository noticiaRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private MistralClient mistralClient;

    @Mock
    private IconifyClient iconifyClient;

    private IlustradorService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JsonMapper();
        service = new IlustradorService(noticiaRepository, geminiClient, mistralClient, iconifyClient, objectMapper);
    }

    private Noticia noticia() {
        return new Noticia("OpenAI lança novo modelo", "https://exemplo.com/1", "fonte", "resumo", Instant.now());
    }

    @Test
    void montaACapaComOTermoEACorEscolhidosPeloGemini() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenReturn("{\"termoIcone\":\"robot\",\"corDestaque\":\"#FF2E9A\"}");
        when(iconifyClient.buscarIconeSvg("robot", "tabler")).thenReturn(ICONE_TABLER);

        String svg = service.regerarIlustracao(noticia);

        assertThat(svg).contains("grade-blog", "brilho-neon", "#171725", "#8CF7FF", "#FF2E9A");
        verify(iconifyClient).buscarIconeSvg("robot", "tabler");
        verify(noticiaRepository).save(noticia);
        assertThat(noticia.getSvgIlustracao()).contains("grade-blog");
    }

    @Test
    void usaOIconeGenericoQuandoOIconifyFalha() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenReturn("{\"termoIcone\":\"algo-inexistente\",\"corDestaque\":\"#00F0FF\"}");
        when(iconifyClient.buscarIconeSvg(anyString(), anyString()))
                .thenThrow(new IllegalStateException("nada encontrado"));

        String svg = service.regerarIlustracao(noticia);

        // ícone de fallback é um "chip" com path fixo, reconhecível pelo trecho abaixo
        assertThat(svg).contains("M6 4h12a2 2 0 0 1 2 2v12");
    }

    @Test
    void passaAUsarOMistralQuandoOGeminiEstouraACota() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenThrow(new LimiteGeminiAtingidoException("cota diária atingida"));
        when(mistralClient.chat(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn("{\"termoIcone\":\"cloud\",\"corDestaque\":\"#9D4EFF\"}");
        when(iconifyClient.buscarIconeSvg("cloud", "tabler")).thenReturn(ICONE_TABLER);

        String svg = service.regerarIlustracao(noticia);

        assertThat(svg).contains("#9D4EFF");
        verify(mistralClient).chat(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void usaOAcabamentoPadraoQuandoGeminiEMistralFalham() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenThrow(new LimiteGeminiAtingidoException("cota diária atingida"));
        when(mistralClient.chat(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true)))
                .thenThrow(new LimiteMistralAtingidoException("orçamento estourado"));
        when(iconifyClient.buscarIconeSvg(anyString(), anyString())).thenReturn(ICONE_TABLER);

        String svg = service.regerarIlustracao(noticia);

        // Acabamento.padrao sempre usa "chip" como termo e uma das 4 cores da paleta do blog
        verify(iconifyClient).buscarIconeSvg("chip", "tabler");
        assertThat(svg).containsAnyOf("#00F0FF", "#8CF7FF", "#FF2E9A", "#9D4EFF");
    }

    @Test
    void ignoraCorDestaqueForaDaPaletaDoBlogEUsaAPrimeiraComoFallback() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenReturn("{\"termoIcone\":\"robot\",\"corDestaque\":\"#ff0000\"}");
        when(iconifyClient.buscarIconeSvg("robot", "tabler")).thenReturn(ICONE_TABLER);

        String svg = service.regerarIlustracao(noticia);

        assertThat(svg).doesNotContain("#ff0000").contains("#00F0FF");
    }

    @Test
    void naoBuscaNoIconifyMaisDeUmaVezPorChamada() {
        Noticia noticia = noticia();
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenReturn("{\"termoIcone\":\"robot\",\"corDestaque\":\"#00F0FF\"}");
        when(iconifyClient.buscarIconeSvg(anyString(), anyString())).thenReturn(ICONE_TABLER);

        service.regerarIlustracao(noticia);

        verify(iconifyClient, times(1)).buscarIconeSvg(any(), any());
    }
}
