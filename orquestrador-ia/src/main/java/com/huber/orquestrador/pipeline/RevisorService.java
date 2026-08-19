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
public class RevisorService {

    private static final Logger log = LoggerFactory.getLogger(RevisorService.class);

    private static final String PROMPT_SISTEMA = """
            Você é um revisor de texto experiente. Revise o post de LinkedIn abaixo:
            - Corrija erros de português e clareza
            - Mantenha o tom e o tamanho originais
            - Não adicione markdown nem comentários
            - Preserve as hashtags no final
            - Não inclua nem sugira imagens, fotos, vídeos ou qualquer elemento visual — mantenha o post só em texto
            Responda apenas com o texto revisado, sem explicações.
            """;

    private static final String REGRA_FONTE_VERIDICA =
            "\n- Confira se a notícia tem fonte verídica e rechecável; se algo parecer especulativo, suavize o tom";
    private static final String REGRA_ESTRUTURA =
            "\n- Garanta que o texto esteja bem estruturado, com começo, meio e fim claros";
    private static final String REGRA_PADRAO_LINKEDIN =
            "\n- Garanta que o texto siga o padrão de posts do LinkedIn (gancho forte, parágrafos curtos, tom "
                    + "profissional e engajador)";

    private final NoticiaRepository noticiaRepository;
    private final ClienteTextoIa clienteTextoIa;
    private final ConfiguracaoService configuracaoService;

    public RevisorService(NoticiaRepository noticiaRepository, ClienteTextoIa clienteTextoIa,
                           ConfiguracaoService configuracaoService) {
        this.noticiaRepository = noticiaRepository;
        this.clienteTextoIa = clienteTextoIa;
        this.configuracaoService = configuracaoService;
    }

    private String montarPromptSistema() {
        StringBuilder prompt = new StringBuilder(PROMPT_SISTEMA);
        if (configuracaoService.isRevisarFonteVeridica()) {
            prompt.append(REGRA_FONTE_VERIDICA);
        }
        if (configuracaoService.isRevisarEstrutura()) {
            prompt.append(REGRA_ESTRUTURA);
        }
        if (configuracaoService.isRevisarPadraoLinkedin()) {
            prompt.append(REGRA_PADRAO_LINKEDIN);
        }
        return prompt.toString();
    }

    public int revisar() {
        return revisar(null);
    }

    public int revisar(Long id) {
        List<Noticia> redigidas = id != null
                ? noticiaRepository.findById(id)
                        .filter(n -> n.getEstado() == EstadoNoticia.REDIGIDA)
                        .map(List::of)
                        .orElseGet(List::of)
                : noticiaRepository.findByEstado(EstadoNoticia.REDIGIDA);
        int revisadas = 0;
        boolean[] usarMistral = {false};
        String promptSistema = montarPromptSistema();

        for (Noticia noticia : redigidas) {
            String revisado;
            try {
                revisado = clienteTextoIa.chat(usarMistral, promptSistema, noticia.getTextoRedigido());
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Parando revisão: {}", e.getMessage());
                break;
            } catch (Exception e) {
                log.warn("Falha ao revisar a notícia {} (deixando como está, tenta de novo depois): {}",
                        noticia.getId(), e.getMessage());
                continue;
            }

            noticia.setTextoRevisado(revisado);
            noticia.mudarEstado(EstadoNoticia.REVISADA);
            noticiaRepository.save(noticia);
            revisadas++;
        }
        return revisadas;
    }
}
