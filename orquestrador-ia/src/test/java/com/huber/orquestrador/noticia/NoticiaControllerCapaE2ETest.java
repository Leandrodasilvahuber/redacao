package com.huber.orquestrador.noticia;

import com.huber.orquestrador.blog.BlogClient;
import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.gemini.GeminiClient;
import com.huber.orquestrador.iconify.IconifyClient;
import com.huber.orquestrador.linkedin.LinkedInClient;
import com.huber.orquestrador.mistral.MistralClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta: sobe o contexto Spring completo (controller -> services -> repositório -> H2 de
 * verdade), só substituindo os clientes que fazem chamada HTTP externa de fato (Gemini, Mistral,
 * Iconify, LinkedIn, Blog). Cobre os 3 endpoints novos de regeneração de capa e os status HTTP
 * corrigidos no code review (400/404/409/502), além do fluxo real de "publica antes de excluir"
 * do LinkedIn.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoticiaControllerCapaE2ETest {

    private static final String ICONE_TABLER =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">"
                    + "<path fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" d=\"M4 4h16v16H4z\"/></svg>";

    private static final String IMAGEM_BASE64 = "aGVsbG8gbXVuZG8="; // "hello mundo"

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private ConfiguracaoService configuracaoService;

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

    @BeforeEach
    void setUp() {
        when(geminiClient.chat(anyString(), anyString(), anyMap()))
                .thenReturn("{\"termoIcone\":\"robot\",\"corDestaque\":\"#00F0FF\"}");
        when(iconifyClient.buscarIconeSvg(anyString(), anyString())).thenReturn(ICONE_TABLER);
    }

    private Noticia salvarNoticia(String link) {
        Noticia noticia = new Noticia("Notícia de teste", link, "fonte", "resumo", Instant.now());
        return noticiaRepository.save(noticia);
    }

    @Test
    void regerarCapaGeraUmSvgNoPadraoDoBlogEPersisteNaNoticia() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/regerar-capa");

        mockMvc.perform(post("/noticias/{id}/regerar-capa", noticia.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.svgIlustracao").value(org.hamcrest.Matchers.containsString("grade-blog")));

        Noticia recarregada = noticiaRepository.findById(noticia.getId()).orElseThrow();
        assertThat(recarregada.getSvgIlustracao()).contains("grade-blog");
    }

    @Test
    void regerarCapaDevolve404ProNoticiaInexistente() throws Exception {
        mockMvc.perform(post("/noticias/{id}/regerar-capa", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void capaBlogDevolve400QuandoFaltaAImagem() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-blog-sem-imagem");

        mockMvc.perform(put("/noticias/{id}/capa-blog", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(blogClient, never()).atualizarCapa(anyString(), anyString());
    }

    @Test
    void capaBlogDevolve409QuandoANoticiaAindaNaoFoiPublicadaNoBlog() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-blog-sem-post");

        mockMvc.perform(put("/noticias/{id}/capa-blog", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isConflict());

        verify(blogClient, never()).atualizarCapa(anyString(), anyString());
    }

    @Test
    void capaBlogAtualizaAImagemDoPostExistente() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-blog-ok");
        noticia.marcarPublicadaNoBlog("777");
        noticiaRepository.save(noticia);

        mockMvc.perform(put("/noticias/{id}/capa-blog", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isOk());

        verify(blogClient).atualizarCapa(eq("777"), anyString());
    }

    @Test
    void capaBlogDevolve502QuandoOBlogFalhaDeVerdade() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-blog-falha");
        noticia.marcarPublicadaNoBlog("778");
        noticiaRepository.save(noticia);
        org.mockito.Mockito.doThrow(new RuntimeException("blog fora do ar"))
                .when(blogClient).atualizarCapa(anyString(), anyString());

        mockMvc.perform(put("/noticias/{id}/capa-blog", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void capaLinkedinDevolve409QuandoOLinkedinNaoEstaConectado() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-linkedin-desconectado");

        mockMvc.perform(put("/noticias/{id}/capa-linkedin", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isConflict());

        verify(linkedInClient, never()).publicarPost(anyString(), anyString(), any(), any());
    }

    @Test
    void capaLinkedinPublicaOPostNovoEExcluiOAntigoNaOrdemCerta() throws Exception {
        configuracaoService.salvarTokenLinkedin("token-teste", Instant.now().plusSeconds(3600), "urn:li:person:teste");
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-linkedin-ok");
        noticia.marcarPublicadaNoLinkedin("urn:li:share:antigo");
        noticiaRepository.save(noticia);
        when(linkedInClient.publicarPost(eq("token-teste"), eq("urn:li:person:teste"), any(), any()))
                .thenReturn("urn:li:share:novo");

        mockMvc.perform(put("/noticias/{id}/capa-linkedin", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedinPostUrn").value("urn:li:share:novo"));

        org.mockito.InOrder ordem = org.mockito.Mockito.inOrder(linkedInClient);
        ordem.verify(linkedInClient).publicarPost(eq("token-teste"), eq("urn:li:person:teste"), any(), any());
        ordem.verify(linkedInClient).excluirPost("token-teste", "urn:li:share:antigo");

        Noticia recarregada = noticiaRepository.findById(noticia.getId()).orElseThrow();
        assertThat(recarregada.getLinkedinPostUrn()).isEqualTo("urn:li:share:novo");
    }

    @Test
    void capaLinkedinDevolve502SemPerderAUrnAntigaQuandoAPublicacaoFalha() throws Exception {
        configuracaoService.salvarTokenLinkedin("token-teste", Instant.now().plusSeconds(3600), "urn:li:person:teste");
        Noticia noticia = salvarNoticia("https://exemplo.com/capa-linkedin-falha");
        noticia.marcarPublicadaNoLinkedin("urn:li:share:antigo");
        noticiaRepository.save(noticia);
        when(linkedInClient.publicarPost(anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("linkedin fora do ar"));

        mockMvc.perform(put("/noticias/{id}/capa-linkedin", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imagemPngBase64\":\"" + IMAGEM_BASE64 + "\"}"))
                .andExpect(status().isBadGateway());

        verify(linkedInClient, never()).excluirPost(anyString(), anyString());
        Noticia recarregada = noticiaRepository.findById(noticia.getId()).orElseThrow();
        assertThat(recarregada.getLinkedinPostUrn()).isEqualTo("urn:li:share:antigo");
    }
}
