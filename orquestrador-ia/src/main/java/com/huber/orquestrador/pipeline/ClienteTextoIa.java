package com.huber.orquestrador.pipeline;

import com.huber.orquestrador.groq.GroqClient;
import com.huber.orquestrador.groq.LimiteGroqAtingidoException;
import com.huber.orquestrador.mistral.MistralClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Usa o Groq primeiro; se a cota diária do Groq estourar, passa a usar o Mistral
 * (não precisa de cartão de crédito) pelo resto da execução do processo atual.
 */
@Component
public class ClienteTextoIa {

    private static final Logger log = LoggerFactory.getLogger(ClienteTextoIa.class);

    private final GroqClient groqClient;
    private final MistralClient mistralClient;

    public ClienteTextoIa(GroqClient groqClient, MistralClient mistralClient) {
        this.groqClient = groqClient;
        this.mistralClient = mistralClient;
    }

    public String chat(boolean[] usarMistral, String systemPrompt, String userPrompt) {
        if (!usarMistral[0]) {
            try {
                return groqClient.chat(systemPrompt, userPrompt);
            } catch (LimiteGroqAtingidoException e) {
                log.warn("Limite do Groq atingido, trocando para Mistral pelo resto da execução: {}", e.getMessage());
                usarMistral[0] = true;
            }
        }
        return mistralClient.chat(systemPrompt, userPrompt);
    }
}
