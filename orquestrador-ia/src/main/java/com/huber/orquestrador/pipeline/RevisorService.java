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
public class RevisorService {

    private static final Logger log = LoggerFactory.getLogger(RevisorService.class);

    private static final String PROMPT_SISTEMA = """
            Você é um revisor de texto experiente. Revise o post de LinkedIn abaixo:
            - Corrija erros de português e clareza
            - Mantenha o tom e o tamanho originais
            - Não adicione markdown nem comentários
            - Preserve as hashtags no final
            Responda apenas com o texto revisado, sem explicações.
            """;

    private final NoticiaRepository noticiaRepository;
    private final GroqClient groqClient;

    public RevisorService(NoticiaRepository noticiaRepository, GroqClient groqClient) {
        this.noticiaRepository = noticiaRepository;
        this.groqClient = groqClient;
    }

    public int revisar() {
        List<Noticia> redigidas = noticiaRepository.findByEstado(EstadoNoticia.REDIGIDA);
        int revisadas = 0;

        for (Noticia noticia : redigidas) {
            String revisado;
            try {
                revisado = groqClient.chat(PROMPT_SISTEMA, noticia.getTextoRedigido());
            } catch (LimiteGroqAtingidoException e) {
                log.warn("Parando revisão: {}", e.getMessage());
                break;
            }

            noticia.setTextoRevisado(revisado);
            noticia.mudarEstado(EstadoNoticia.REVISADA);
            noticiaRepository.save(noticia);
            revisadas++;
        }
        return revisadas;
    }
}
