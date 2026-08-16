package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.groq.GroqClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeletorService {

    private static final String PROMPT_SISTEMA = """
            Você é um editor de tecnologia que decide quais notícias merecem virar um post no LinkedIn
            para um público de profissionais de TI. Responda APENAS com a palavra SIM ou NÃO na primeira linha,
            indicando se a notícia é relevante, interessante e tem potencial de gerar boa discussão profissional.
            """;

    private final NoticiaRepository noticiaRepository;
    private final GroqClient groqClient;
    private final int limiteDiario;

    public SeletorService(NoticiaRepository noticiaRepository,
                           GroqClient groqClient,
                           @Value("${selecao.limite-diario}") int limiteDiario) {
        this.noticiaRepository = noticiaRepository;
        this.groqClient = groqClient;
        this.limiteDiario = limiteDiario;
    }

    public int selecionar() {
        List<Noticia> pendentes = noticiaRepository.findByEstado(EstadoNoticia.BUSCADA);
        long jaSelecionadasHoje = noticiaRepository.findByEstado(EstadoNoticia.SELECIONADA).size();
        int selecionadas = 0;

        for (Noticia noticia : pendentes) {
            if (jaSelecionadasHoje + selecionadas >= limiteDiario) {
                break;
            }

            String pergunta = "Título: " + noticia.getTitulo() + "\nResumo: " + noticia.getResumoOriginal();
            String resposta = groqClient.chat(PROMPT_SISTEMA, pergunta);

            if (resposta.toUpperCase().startsWith("SIM")) {
                noticia.mudarEstado(EstadoNoticia.SELECIONADA);
                selecionadas++;
            } else {
                noticia.mudarEstado(EstadoNoticia.DESCARTADA);
            }
            noticiaRepository.save(noticia);
            Pausa.aguardar();
        }
        return selecionadas;
    }
}
