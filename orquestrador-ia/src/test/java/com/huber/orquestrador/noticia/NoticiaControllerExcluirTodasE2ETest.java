package com.huber.orquestrador.noticia;

import com.huber.orquestrador.blog.BlogClient;
import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.iconify.IconifyClient;
import com.huber.orquestrador.linkedin.LinkedInClient;
import com.huber.orquestrador.mistral.MistralClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ponta a ponta do "Excluir todos" por coluna (DELETE /noticias?estado=...). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoticiaControllerExcluirTodasE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticiaRepository noticiaRepository;

    @MockitoBean
    private GeminiClient geminiClient;

    @MockitoBean
    private MistralClient mistralClient;

    @MockitoBean
    private IconifyClient iconifyClient;

    @MockitoBean
    private LinkedInClient linkedInClient;

    @MockitoBean
    private BlogClient blogClient;

    private Noticia salvarNoticia(String link, EstadoNoticia estado) {
        Noticia noticia = new Noticia("Notícia de teste", link, "fonte", "resumo", Instant.now());
        noticia.mudarEstado(estado);
        return noticiaRepository.save(noticia);
    }

    @Test
    void excluiTodasAsNoticiasDoEstadoInformado() throws Exception {
        salvarNoticia("https://exemplo.com/1", EstadoNoticia.BUSCADA);
        salvarNoticia("https://exemplo.com/2", EstadoNoticia.BUSCADA);
        salvarNoticia("https://exemplo.com/3", EstadoNoticia.SELECIONADA);

        mockMvc.perform(delete("/noticias").param("estado", "BUSCADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excluidas").value(2));

        assertThat(noticiaRepository.findByEstado(EstadoNoticia.BUSCADA)).isEmpty();
        assertThat(noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA)).hasSize(1);
    }

    @Test
    void naoExcluiNoticiasPublicadasEmMassa() throws Exception {
        salvarNoticia("https://exemplo.com/publicada", EstadoNoticia.PUBLICADA);

        mockMvc.perform(delete("/noticias").param("estado", "PUBLICADA"))
                .andExpect(status().isBadRequest());

        assertThat(noticiaRepository.findByEstado(EstadoNoticia.PUBLICADA)).hasSize(1);
        verify(linkedInClient, never()).excluirPost(any(), any());
    }

    @Test
    void devolveZeroQuandoNaoHaNoticiasNoEstado() throws Exception {
        mockMvc.perform(delete("/noticias").param("estado", "REDIGIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excluidas").value(0));
    }
}
