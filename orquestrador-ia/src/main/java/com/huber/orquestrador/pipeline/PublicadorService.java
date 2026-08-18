package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicadorService {

    private static final Logger log = LoggerFactory.getLogger(PublicadorService.class);

    private static final String PROMPT_SISTEMA = """
            Você prepara posts para publicação no LinkedIn. Ajuste o texto abaixo para o formato final:
            - Garanta que as quebras de linha entre parágrafos estejam corretas para colar direto no LinkedIn
            - Garanta que o texto tenha no máximo 3000 caracteres
            - Garanta que as hashtags estejam na última linha, separadas por espaço
            - Não altere o conteúdo nem o tom, só a formatação
            - Não inclua nem sugira imagens, fotos, vídeos ou qualquer elemento visual — o resultado é só texto
            Responda apenas com o texto final pronto para publicar, sem explicações.
            """;

    private final NoticiaRepository noticiaRepository;
    private final ClienteTextoIa clienteTextoIa;

    public PublicadorService(NoticiaRepository noticiaRepository, ClienteTextoIa clienteTextoIa) {
        this.noticiaRepository = noticiaRepository;
        this.clienteTextoIa = clienteTextoIa;
    }

    public int publicar() {
        return publicar(null);
    }

    public int publicar(Long id) {
        List<Noticia> ilustradas = id != null
                ? noticiaRepository.findById(id)
                        .filter(n -> n.getEstado() == EstadoNoticia.ILUSTRADA)
                        .map(List::of)
                        .orElseGet(List::of)
                : noticiaRepository.findByEstado(EstadoNoticia.ILUSTRADA);
        int prontas = 0;
        boolean[] usarMistral = {false};

        for (Noticia noticia : ilustradas) {
            String textoFinal;
            try {
                textoFinal = clienteTextoIa.chat(usarMistral, PROMPT_SISTEMA, noticia.getTextoIlustrado());
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Parando formatação final: {}", e.getMessage());
                break;
            }

            noticia.setTextoFinal(textoFinal);
            noticia.mudarEstado(EstadoNoticia.PRONTA_PARA_PUBLICAR);
            noticiaRepository.save(noticia);
            prontas++;
        }
        return prontas;
    }
}
