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
            String estiloIlustracao,
            String provedorIlustracao,
            String blogApiUrl,
            String blogApiToken,
            String blogIlustracaoPadrao,
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
            String estiloIlustracao,
            String provedorIlustracao,
            String blogApiUrl,
            String blogApiTokenMascarado,
            boolean blogApiTokenConfigurado,
            String blogIlustracaoPadrao,
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
