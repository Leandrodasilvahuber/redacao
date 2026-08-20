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

    /** Último termo do histórico JSON (ex.: {@code ["robot","chip"]} -> {@code "chip"}). */
    private static String ultimoTermoDoHistorico(Noticia noticia) {
        String semColchetes = noticia.getTermosIconeUsados().replaceAll("[\\[\\]\"]", "");
        String[] partes = semColchetes.split(",");
        return partes[partes.length - 1].trim();
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
    void regerarIconeMantemACorDeDestaqueAtualETrocaOIcone() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/regerar-icone");
        noticia.setSvgIlustracao(
                "[\"<svg xmlns=\\\"http://www.w3.org/2000/svg\\\" viewBox=\\\"0 0 1200 627\\\">"
                        + "<circle cx=\\\"862.4\\\" cy=\\\"170.6\\\" r=\\\"9\\\" fill=\\\"#FF2E9A\\\"/>"
                        + "<circle cx=\\\"336.8\\\" cy=\\\"460.0\\\" r=\\\"6\\\" fill=\\\"#9D4EFF\\\"/></svg>\"]");
        noticiaRepository.save(noticia);

        mockMvc.perform(post("/noticias/{id}/regerar-icone", noticia.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.svgIlustracao").value(org.hamcrest.Matchers.containsString("#FF2E9A")));

        Noticia recarregada = noticiaRepository.findById(noticia.getId()).orElseThrow();
        assertThat(recarregada.getSvgIlustracao()).contains("#FF2E9A").contains("grade-blog");
    }

    @Test
    void regerarIconeDevolve404ProNoticiaInexistente() throws Exception {
        mockMvc.perform(post("/noticias/{id}/regerar-icone", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void regerarIconeTresVezesSeguidasNaoRepeteNenhumTermoDoHistoricoQuandoAIaInsisteEmTermoJaUsado() throws Exception {
        // o mock do Gemini sempre devolve o mesmo termoIcone ("robot"), simulando a IA insistindo
        Noticia noticia = salvarNoticia("https://exemplo.com/regerar-icone-tres-vezes");

        mockMvc.perform(post("/noticias/{id}/regerar-icone", noticia.getId()))
                .andExpect(status().isOk());
        Noticia apos1a = noticiaRepository.findById(noticia.getId()).orElseThrow();
        String termo1 = ultimoTermoDoHistorico(apos1a);
        assertThat(termo1).isEqualToIgnoringCase("robot");

        mockMvc.perform(post("/noticias/{id}/regerar-icone", noticia.getId()))
                .andExpect(status().isOk());
        Noticia apos2a = noticiaRepository.findById(noticia.getId()).orElseThrow();
        String termo2 = ultimoTermoDoHistorico(apos2a);
        assertThat(termo2).isNotEqualToIgnoringCase(termo1);

        mockMvc.perform(post("/noticias/{id}/regerar-icone", noticia.getId()))
                .andExpect(status().isOk());
        Noticia apos3a = noticiaRepository.findById(noticia.getId()).orElseThrow();
        String termo3 = ultimoTermoDoHistorico(apos3a);
        assertThat(termo3).isNotEqualToIgnoringCase(termo1).isNotEqualToIgnoringCase(termo2);
    }

    @Test
    void regerarIconeEnviaDescricaoDoUsuarioParaAIa() throws Exception {
        Noticia noticia = salvarNoticia("https://exemplo.com/regerar-icone-descricao");

        mockMvc.perform(post("/noticias/{id}/regerar-icone", noticia.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"algo específico\"}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<String> pedidoCapturado = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(geminiClient).chat(anyString(), pedidoCapturado.capture(), anyMap());
        assertThat(pedidoCapturado.getValue()).contains("algo específico");
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
