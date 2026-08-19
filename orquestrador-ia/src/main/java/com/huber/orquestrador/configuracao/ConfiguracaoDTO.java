package com.huber.orquestrador.configuracao;

import java.util.List;

public class ConfiguracaoDTO {

    public record Request(
            String groqApiKey,
            String geminiApiKey,
            String mistralApiKey,
            List<String> criteriosBusca,
            boolean revisarFonteVeridica,
            boolean revisarEstrutura,
            boolean revisarPadraoLinkedin,
            boolean atribuirFonte,
            String blogApiUrl,
            String blogApiToken,
            String bibliotecaIcones,
            Integer cotaGroq,
            Integer cotaGemini,
            Integer cotaMistral,
            String linkedinClientId,
            String linkedinClientSecret
    ) {
    }

    public record Response(
            String groqApiKeyMascarada,
            boolean groqApiKeyConfigurada,
            String geminiApiKeyMascarada,
            boolean geminiApiKeyConfigurada,
            String mistralApiKeyMascarada,
            boolean mistralApiKeyConfigurada,
            List<String> criteriosBusca,
            boolean revisarFonteVeridica,
            boolean revisarEstrutura,
            boolean revisarPadraoLinkedin,
            boolean atribuirFonte,
            String blogApiUrl,
            String blogApiTokenMascarado,
            boolean blogApiTokenConfigurado,
            String bibliotecaIcones,
            int cotaGroq,
            int cotaGemini,
            int cotaMistral,
            String linkedinClientIdMascarado,
            boolean linkedinClientIdConfigurado,
            String linkedinClientSecretMascarado,
            boolean linkedinClientSecretConfigurado,
            boolean linkedinConectado,
            String linkedinPersonUrn,
            String linkedinTokenExpiraEm
    ) {
    }

    private ConfiguracaoDTO() {
    }
}
