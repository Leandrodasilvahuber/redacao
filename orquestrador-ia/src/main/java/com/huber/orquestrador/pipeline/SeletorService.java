package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.configuracao.CriterioBusca;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import com.huber.orquestrador.noticia.PromocaoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeletorService {

    private static final Logger log = LoggerFactory.getLogger(SeletorService.class);

    private static final String PROMPT_SISTEMA = """
            Você é um editor de tecnologia que decide quais notícias merecem virar um post no LinkedIn
            para um público de profissionais de TI. Responda APENAS com a palavra SIM ou NÃO na primeira linha,
            indicando se a notícia é relevante, interessante e tem potencial de gerar boa discussão profissional.
            Responda NÃO para publicidade, publieditorial, conteúdo patrocinado, comparativos de preço,
            listas de "melhores ofertas/produtos para comprar" ou qualquer texto cujo objetivo principal seja
            vender ou promover um produto específico, mesmo que mencione tecnologia.
            """;

    private final NoticiaRepository noticiaRepository;
    private final ClienteTextoIa clienteTextoIa;
    private final ConfiguracaoService configuracaoService;
    private final int limiteDiario;

    public SeletorService(NoticiaRepository noticiaRepository,
                           ClienteTextoIa clienteTextoIa,
                           ConfiguracaoService configuracaoService,
                           @Value("${selecao.limite-diario}") int limiteDiario) {
        this.noticiaRepository = noticiaRepository;
        this.clienteTextoIa = clienteTextoIa;
        this.configuracaoService = configuracaoService;
        this.limiteDiario = limiteDiario;
    }

    private String montarPromptSistema(String termo) {
        List<CriterioBusca> criterios = configuracaoService.getCriteriosBuscaAtivos();
        String prompt = PROMPT_SISTEMA;
        if (!criterios.isEmpty()) {
            String lista = criterios.stream().map(CriterioBusca::getRotulo).collect(Collectors.joining(", "));
            prompt += "\nPriorize especialmente notícias do tipo: " + lista + ".";
        }
        if (termo != null && !termo.isBlank()) {
            prompt += "\nPrioridade adicional pedida pelo usuário nesta busca: \"" + termo.trim()
                    + "\". Dê preferência a notícias relacionadas a esse tema, mas continue avaliando pelos"
                    + " critérios acima.";
        }
        return prompt;
    }

    public int selecionar() {
        return selecionar(null, null);
    }

    public int selecionar(Long id) {
        return selecionar(id, null);
    }

    public int selecionar(Long id, String termo) {
        List<Noticia> pendentes = id != null
                ? noticiaRepository.findById(id)
                        .filter(n -> n.getEstado() == EstadoNoticia.BUSCADA)
                        .map(List::of)
                        .orElseGet(List::of)
                : noticiaRepository.findByEstado(EstadoNoticia.BUSCADA);
        long jaSelecionadasHoje = noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA).size();
        int selecionadas = 0;
        boolean[] usarMistral = {false};
        String promptSistema = montarPromptSistema(termo);

        for (Noticia noticia : pendentes) {
            if (jaSelecionadasHoje + selecionadas >= limiteDiario) {
                break;
            }

            if (PromocaoUtil.pareceAnuncioOuPromocao(noticia.getTitulo())) {
                log.info("Descartando notícia promocional/publicitária sem gastar chamada de IA: {}", noticia.getTitulo());
                noticia.mudarEstado(EstadoNoticia.DESCARTADA);
                noticiaRepository.save(noticia);
                continue;
            }

            String pergunta = "Título: " + noticia.getTitulo() + "\nResumo: " + noticia.getResumoOriginal();
            String resposta;
            try {
                resposta = clienteTextoIa.chat(usarMistral, promptSistema, pergunta);
            } catch (LimiteMistralAtingidoException e) {
                log.warn("Parando seleção: {}", e.getMessage());
                break;
            } catch (Exception e) {
                log.warn("Falha ao selecionar a notícia {} (deixando como está, tenta de novo depois): {}",
                        noticia.getId(), e.getMessage());
                continue;
            }

            if (resposta.toUpperCase().startsWith("SIM")) {
                noticia.mudarEstado(EstadoNoticia.SELECIONADA);
                selecionadas++;
            } else {
                noticia.mudarEstado(EstadoNoticia.DESCARTADA);
            }
            noticiaRepository.save(noticia);
        }
        return selecionadas;
    }
}
