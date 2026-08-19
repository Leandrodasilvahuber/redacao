package com.huber.orquestrador.pipeline;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import com.huber.orquestrador.noticia.TituloSimilaridadeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BuscadorService {

    private static final Logger log = LoggerFactory.getLogger(BuscadorService.class);

    private final NoticiaRepository noticiaRepository;
    private final List<String> feeds;

    public BuscadorService(NoticiaRepository noticiaRepository,
                            @Value("${rss.feeds}") List<String> feeds) {
        this.noticiaRepository = noticiaRepository;
        this.feeds = feeds;
    }

    /**
     * Além de ignorar links já vistos, evita gravar duas vezes a mesma notícia quando ela é
     * replicada com manchetes diferentes por fontes distintas (comparando as palavras do título).
     */
    public int buscar() {
        List<Set<String>> titulosExistentes = new ArrayList<>();
        for (String titulo : noticiaRepository.findAllTitulos()) {
            titulosExistentes.add(TituloSimilaridadeUtil.normalizar(titulo));
        }

        int novas = 0;
        for (String feedUrl : feeds) {
            try {
                novas += buscarFeed(feedUrl, titulosExistentes);
            } catch (Exception e) {
                log.warn("Falha ao ler feed {}: {}", feedUrl, e.getMessage());
            }
        }
        return novas;
    }

    private int buscarFeed(String feedUrl, List<Set<String>> titulosExistentes) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));
        int novas = 0;

        for (SyndEntry entry : feed.getEntries()) {
            if (noticiaRepository.existsByLink(entry.getLink())) {
                continue;
            }

            Set<String> palavrasTitulo = TituloSimilaridadeUtil.normalizar(entry.getTitle());
            boolean duplicada = titulosExistentes.stream()
                    .anyMatch(palavras -> TituloSimilaridadeUtil.saoDuplicados(palavras, palavrasTitulo));
            if (duplicada) {
                log.info("Ignorando notícia repetida (mesmo assunto de outra fonte): {}", entry.getTitle());
                continue;
            }

            String resumo = entry.getDescription() != null ? entry.getDescription().getValue() : "";
            Instant dataPublicacao = entry.getPublishedDate() != null
                    ? entry.getPublishedDate().toInstant()
                    : Instant.now();

            Noticia noticia = new Noticia(entry.getTitle(), entry.getLink(), feed.getTitle(), resumo, dataPublicacao);
            noticiaRepository.save(noticia);
            titulosExistentes.add(palavrasTitulo);
            novas++;
        }
        return novas;
    }
}
