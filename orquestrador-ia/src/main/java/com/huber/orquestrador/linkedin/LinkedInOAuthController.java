package com.huber.orquestrador.linkedin;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class LinkedInOAuthController {

    private static final Logger log = LoggerFactory.getLogger(LinkedInOAuthController.class);

    private final LinkedInClient linkedInClient;
    private final ConfiguracaoService configuracaoService;
    private final LinkedInProperties properties;

    public LinkedInOAuthController(LinkedInClient linkedInClient,
                                    ConfiguracaoService configuracaoService,
                                    LinkedInProperties properties) {
        this.linkedInClient = linkedInClient;
        this.configuracaoService = configuracaoService;
        this.properties = properties;
    }

    @GetMapping("/linkedin/conectar")
    public void conectar(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();
        String url = linkedInClient.construirUrlAutorizacao(state);
        log.info("Redirecionando para autorização do LinkedIn: {}", url);
        response.sendRedirect(url);
    }

    @GetMapping("/linkedin/callback")
    public void callback(@RequestParam(required = false) String code,
                          @RequestParam(required = false) String error,
                          @RequestParam(required = false) String error_description,
                          HttpServletResponse response) throws IOException {
        if (error != null || code == null) {
            log.warn("LinkedIn retornou erro no callback: error={}, descricao={}", error, error_description);
            response.sendRedirect(properties.getFrontendUrl() + "/?linkedin=erro&motivo="
                    + URLEncoder.encode(error_description != null ? error_description : "sem código", StandardCharsets.UTF_8));
            return;
        }

        try {
            LinkedInClient.TrocaTokenResponse token = linkedInClient.trocarCodigoPorToken(code);
            String personUrn = linkedInClient.obterPersonUrn(token.accessToken());
            configuracaoService.salvarTokenLinkedin(token.accessToken(), token.calcularExpiracao(), personUrn);
            response.sendRedirect(properties.getFrontendUrl() + "/?linkedin=conectado");
        } catch (Exception e) {
            log.warn("Falha ao trocar código por token do LinkedIn: {}", e.getMessage(), e);
            response.sendRedirect(properties.getFrontendUrl() + "/?linkedin=erro&motivo="
                    + URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "erro desconhecido", StandardCharsets.UTF_8));
        }
    }
}
