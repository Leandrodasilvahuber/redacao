package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscadorServiceTest {

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Feed de teste</title>
                <item>
                  <title>Python 3.13 traz novidades para desenvolvedores</title>
                  <link>https://exemplo.com/python-3-13</link>
                  <description>Nova versão do Python chega com melhorias de desempenho.</description>
                </item>
                <item>
                  <title>Ofertas imperdíveis de notebooks na Black Friday</title>
                  <link>https://exemplo.com/black-friday-notebooks</link>
                  <description>Confira os melhores preços em notebooks.</description>
                </item>
              </channel>
            </rss>
            """;

    @Mock
    private NoticiaRepository noticiaRepository;

    private HttpServer servidor;
    private BuscadorService service;

    @BeforeEach
    void setUp() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/rss", exchange -> {
            byte[] corpo = RSS.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/rss+xml; charset=UTF-8");
            exchange.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = exchange.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.start();
        String feedUrl = "http://localhost:" + servidor.getAddress().getPort() + "/rss";
        service = new BuscadorService(noticiaRepository, List.of(feedUrl));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void semTermoImportaTodasAsEntradasNaoDuplicadas() {
        when(noticiaRepository.findAllTitulos()).thenReturn(List.of());
        when(noticiaRepository.existsByLink(anyString())).thenReturn(false);

        int novas = service.buscar();

        assertThat(novas).isEqualTo(2);
    }

    @Test
    void comTermoSoImportaEntradasCujoTituloOuResumoContenhamOTermo() {
        when(noticiaRepository.findAllTitulos()).thenReturn(List.of());
        when(noticiaRepository.existsByLink(anyString())).thenReturn(false);

        ArgumentCaptor<Noticia> salva = ArgumentCaptor.forClass(Noticia.class);
        int novas = service.buscar("python");

        assertThat(novas).isEqualTo(1);
        org.mockito.Mockito.verify(noticiaRepository).save(salva.capture());
        assertThat(salva.getValue().getTitulo()).contains("Python");
    }

    @Test
    void oTermoIgnoraAcentosEMaiusculas() {
        when(noticiaRepository.findAllTitulos()).thenReturn(List.of());
        when(noticiaRepository.existsByLink(anyString())).thenReturn(false);

        int novas = service.buscar("NOTEBOOKS");

        assertThat(novas).isEqualTo(1);
    }

    @Test
    void termoSemNenhumaCorrespondenciaNaoImportaNada() {
        when(noticiaRepository.findAllTitulos()).thenReturn(List.of());

        int novas = service.buscar("inteligência artificial generativa");

        assertThat(novas).isEqualTo(0);
        org.mockito.Mockito.verify(noticiaRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
