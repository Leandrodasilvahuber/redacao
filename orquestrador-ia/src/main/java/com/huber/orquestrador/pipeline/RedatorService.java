package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.groq.GroqClient;
import com.huber.orquestrador.groq.LimiteGroqAtingidoException;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedatorService {

    private static final Logger log = LoggerFactory.getLogger(RedatorService.class);

    private static final String PROMPT_SISTEMA = """
            Você é um redator de posts para LinkedIn, especializado em tecnologia.
            Escreva um post original em português, baseado na notícia fornecida, seguindo estas regras:
            - Primeira linha deve ser um gancho curto que prenda a atenção
            - Parágrafos curtos, com quebra de linha entre eles (sem markdown, sem asteriscos)
            - Tom profissional, mas acessível e com opinião
            - Entre 800 e 1300 caracteres
            - Termine com 3 a 5 hashtags relevantes
            Responda apenas com o texto final do post, sem comentários adicionais.
            """;

    private final NoticiaRepository noticiaRepository;
    private final GroqClient groqClient;

    public RedatorService(NoticiaRepository noticiaRepository, GroqClient groqClient) {
        this.noticiaRepository = noticiaRepository;
        this.groqClient = groqClient;
    }

    public int redigir() {
        List<Noticia> selecionadas = noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA);
        int redigidas = 0;

        for (Noticia noticia : selecionadas) {
            String pergunta = "Título: " + noticia.getTitulo()
                    + "\nFonte: " + noticia.getFonte()
                    + "\nResumo: " + noticia.getResumoOriginal()
                    + "\nLink original: " + noticia.getLink();

            String post;
            try {
                post = groqClient.chat(PROMPT_SISTEMA, pergunta);
            } catch (LimiteGroqAtingidoException e) {
                log.warn("Parando redação: {}", e.getMessage());
                break;
            }

            noticia.setTextoRedigido(post);
            noticia.mudarEstado(EstadoNoticia.REDIGIDA);
            noticiaRepository.save(noticia);
            redigidas++;
        }
        return redigidas;
    }
}
