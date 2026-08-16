package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.groq.GroqClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicadorService {

    private static final String PROMPT_SISTEMA = """
            Você prepara posts para publicação no LinkedIn. Ajuste o texto abaixo para o formato final:
            - Garanta que as quebras de linha entre parágrafos estejam corretas para colar direto no LinkedIn
            - Garanta que o texto tenha no máximo 3000 caracteres
            - Garanta que as hashtags estejam na última linha, separadas por espaço
            - Não altere o conteúdo nem o tom, só a formatação
            Responda apenas com o texto final pronto para publicar, sem explicações.
            """;

    private final NoticiaRepository noticiaRepository;
    private final GroqClient groqClient;

    public PublicadorService(NoticiaRepository noticiaRepository, GroqClient groqClient) {
        this.noticiaRepository = noticiaRepository;
        this.groqClient = groqClient;
    }

    public int publicar() {
        List<Noticia> revisadas = noticiaRepository.findByEstado(EstadoNoticia.REVISADA);
        int prontas = 0;

        for (Noticia noticia : revisadas) {
            String textoFinal = groqClient.chat(PROMPT_SISTEMA, noticia.getTextoRevisado());

            noticia.setTextoFinal(textoFinal);
            noticia.mudarEstado(EstadoNoticia.PRONTA_PARA_PUBLICAR);
            noticiaRepository.save(noticia);
            prontas++;
            Pausa.aguardar();
        }
        return prontas;
    }
}
