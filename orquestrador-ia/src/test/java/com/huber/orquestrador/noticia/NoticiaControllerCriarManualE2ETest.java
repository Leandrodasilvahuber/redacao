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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta do fluxo "Criar Notícia": formatar um texto colado com o Mistral (sem salvar) e
 * salvar a notícia já formatada direto no estado REVISADA.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoticiaControllerCriarManualE2ETest {

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

    @Test
    void formatarManualDevolveTituloETextoGeradosPeloMistralSemSalvarNada() throws Exception {
        when(mistralClient.chat(anyString(), anyString(), anyBoolean()))
                .thenReturn("{\"titulo\":\"Nova IA chega ao mercado\",\"texto\":\"Corpo da notícia formatado.\"}");

        mockMvc.perform(post("/noticias/formatar-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"texto colado bruto\",\"tipo\":\"NOTICIA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nova IA chega ao mercado"))
                .andExpect(jsonPath("$.texto").value("Corpo da notícia formatado."));

        assertThat(noticiaRepository.findAll()).isEmpty();
    }

    @Test
    void formatarManualDevolve400QuandoFaltaOTexto() throws Exception {
        mockMvc.perform(post("/noticias/formatar-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"NOTICIA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void formatarManualDevolve400ParaTipoInvalido() throws Exception {
        mockMvc.perform(post("/noticias/formatar-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"texto colado\",\"tipo\":\"ROMANCE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarManualSalvaANoticiaNoEstadoRevisada() throws Exception {
        mockMvc.perform(post("/noticias/criar-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Título manual\",\"texto\":\"Texto já formatado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Título manual"))
                .andExpect(jsonPath("$.estado").value("REVISADA"));

        assertThat(noticiaRepository.findAll()).hasSize(1);
        Noticia salva = noticiaRepository.findAll().get(0);
        assertThat(salva.getTextoRevisado()).isEqualTo("Texto já formatado");
        assertThat(salva.getFonte()).isEqualTo("Manual");
        assertThat(salva.getLink()).startsWith("manual:");
    }

    @Test
    void criarManualDevolve400QuandoFaltaTituloOuTexto() throws Exception {
        mockMvc.perform(post("/noticias/criar-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"texto\":\"Texto\"}"))
                .andExpect(status().isBadRequest());

        assertThat(noticiaRepository.findAll()).isEmpty();
    }
}
