package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
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
            - Não sugira, descreva ou mencione imagens, fotos, vídeos ou qualquer elemento visual — o post é só texto
            Responda apenas com o texto final do post, sem comentários adicionais.
            """;

    private static final String REGRA_ATRIBUIR_FONTE =
            "\n- Cite a fonte original da notícia de forma natural no texto (ex: 'segundo a {fonte}')";

    private final NoticiaRepository noticiaRepository;
    private final ClienteTextoIa clienteTextoIa;
    private final ConfiguracaoService configuracaoService;

    public RedatorService(NoticiaRepository noticiaRepository, ClienteTextoIa clienteTextoIa,
                           ConfiguracaoService configuracaoService) {
        this.noticiaRepository = noticiaRepository;
        this.clienteTextoIa = clienteTextoIa;
        this.configuracaoService = configuracaoService;
    }

    private String montarPromptSistema() {
        if (!configuracaoService.isAtribuirFonte()) {
            return PROMPT_SISTEMA;
        }
        return PROMPT_SISTEMA + REGRA_ATRIBUIR_FONTE;
    }

    public int redigir() {
        return redigir(null);
    }

    public int redigir(Long id) {
        List<Noticia> selecionadas = id != null
                ? noticiaRepository.findById(id)
                        .filter(n -> n.getEstado() == EstadoNoticia.SELECIONADA)
                        .map(List::of)
                        .orElseGet(List::of)
                : noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA);
        int redigidas = 0;
        boolean[] usarMistral = {false};
        String promptSistema = montarPromptSistema();

        for (Noticia noticia : selecionadas) {
            String pergunta = "Título: " + noticia.getTitulo()
                    + "\nFonte: " + noticia.getFonte()
                    + "\nResumo: " + noticia.getResumoOriginal()
                    + "\nLink original: " + noticia.getLink();

            String post;
            try {
                post = clienteTextoIa.chat(usarMistral, promptSistema, pergunta);
            } catch (LimiteMistralAtingidoException e) {
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
