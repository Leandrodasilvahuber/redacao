package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.mistral.MistralClient;
import com.huber.orquestrador.noticia.EstadoNoticia;
import com.huber.orquestrador.noticia.Noticia;
import com.huber.orquestrador.noticia.NoticiaRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Cria notícias manualmente a partir de um texto colado pelo usuário: o Mistral formata o texto
 * em uma notícia curta ou um breve tutorial (a critério do usuário), e o resultado entra no
 * pipeline normal a partir do estado REVISADA, como se tivesse passado por redação e revisão.
 */
@Service
public class CriadorManualService {

    public enum TipoFormatacao {
        NOTICIA, TUTORIAL
    }

    private static final String PROMPT_NOTICIA = """
            Você transforma o texto colado abaixo em uma notícia de tecnologia curta e objetiva,
            pronta para virar post de LinkedIn: parágrafos curtos, sem markdown, tom jornalístico,
            direto ao ponto, preservando os fatos do texto original sem inventar informação nova.
            Responda em JSON válido e apenas o JSON (sem texto fora dele, sem markdown), com
            exatamente estas chaves: "titulo" (string, título curto e chamativo) e "texto" (string,
            o corpo da notícia formatado).
            """;

    private static final String PROMPT_TUTORIAL = """
            Você transforma o texto colado abaixo em um breve tutorial passo a passo sobre
            tecnologia, direto ao ponto, sem markdown, com os passos descritos em frases curtas e
            numeradas, preservando as informações do texto original sem inventar nada novo.
            Responda em JSON válido e apenas o JSON (sem texto fora dele, sem markdown), com
            exatamente estas chaves: "titulo" (string, título curto) e "texto" (string, o corpo do
            tutorial formatado).
            """;

    public record FormatoGerado(String titulo, String texto) {
    }

    private final MistralClient mistralClient;
    private final NoticiaRepository noticiaRepository;
    private final ObjectMapper objectMapper;

    public CriadorManualService(MistralClient mistralClient, NoticiaRepository noticiaRepository,
                                 ObjectMapper objectMapper) {
        this.mistralClient = mistralClient;
        this.noticiaRepository = noticiaRepository;
        this.objectMapper = objectMapper;
    }

    /** Pede ao Mistral pra formatar o texto colado no formato escolhido, sem persistir nada ainda. */
    public FormatoGerado formatar(String textoColado, TipoFormatacao tipo) {
        String prompt = tipo == TipoFormatacao.TUTORIAL ? PROMPT_TUTORIAL : PROMPT_NOTICIA;
        String json = mistralClient.chat(prompt, textoColado, true);
        Map<?, ?> dados = objectMapper.readValue(json, Map.class);
        String titulo = textoSeguro(dados.get("titulo"));
        String texto = textoSeguro(dados.get("texto"));
        if (titulo.isBlank() || texto.isBlank()) {
            throw new IllegalStateException("A IA não devolveu título e texto formatados");
        }
        return new FormatoGerado(titulo, texto);
    }

    /**
     * Salva a notícia manual já no estado REVISADA, pronta pra seguir o pipeline normal
     * (ilustrar, publicar). Usa um link sintético único, já que não existe uma URL de origem.
     */
    public Noticia salvar(String titulo, String texto) {
        String link = "manual:" + UUID.randomUUID();
        Noticia noticia = new Noticia(titulo, link, "Manual", texto, Instant.now());
        noticia.setTextoRevisado(texto);
        noticia.mudarEstado(EstadoNoticia.REVISADA);
        return noticiaRepository.save(noticia);
    }

    private static String textoSeguro(Object valor) {
        return valor instanceof String s ? s.trim() : "";
    }
}
