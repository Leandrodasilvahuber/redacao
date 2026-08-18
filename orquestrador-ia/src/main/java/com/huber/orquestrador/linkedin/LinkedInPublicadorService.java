package com.huber.orquestrador.linkedin;

import com.huber.orquestrador.configuracao.ConfiguracaoService;
import com.huber.orquestrador.noticia.Noticia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LinkedInPublicadorService {

    private static final Logger log = LoggerFactory.getLogger(LinkedInPublicadorService.class);

    private final LinkedInClient linkedInClient;
    private final ConfiguracaoService configuracaoService;

    public LinkedInPublicadorService(LinkedInClient linkedInClient, ConfiguracaoService configuracaoService) {
        this.linkedInClient = linkedInClient;
        this.configuracaoService = configuracaoService;
    }

    public void publicar(Noticia noticia, byte[] imagemPng) {
        try {
            String accessToken = configuracaoService.getLinkedinAccessToken();
            String personUrn = configuracaoService.getLinkedinPersonUrn();
            Instant expiraEm = configuracaoService.getLinkedinTokenExpiraEm();

            if (accessToken == null || accessToken.isBlank() || personUrn == null || personUrn.isBlank()) {
                throw new IllegalStateException(
                        "LinkedIn não está conectado. Acesse Configurações e clique em \"Conectar LinkedIn\".");
            }
            if (expiraEm != null && Instant.now().isAfter(expiraEm)) {
                throw new IllegalStateException(
                        "O acesso ao LinkedIn expirou. Acesse Configurações e reconecte o LinkedIn.");
            }

            String postUrn = linkedInClient.publicarPost(accessToken, personUrn, noticia.getTextoFinal(), imagemPng);
            noticia.marcarPublicadaNoLinkedin(postUrn);
        } catch (Exception e) {
            log.warn("Falha ao publicar notícia {} no LinkedIn: {}", noticia.getId(), e.getMessage());
            noticia.marcarErroLinkedin(e.getMessage());
        }
    }

    public void excluir(Noticia noticia) {
        if (noticia.getLinkedinPostUrn() == null || noticia.getLinkedinPostUrn().isBlank()) {
            return;
        }
        try {
            String accessToken = configuracaoService.getLinkedinAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                log.warn("Não foi possível excluir o post {} do LinkedIn: LinkedIn não está conectado.",
                        noticia.getLinkedinPostUrn());
                return;
            }
            linkedInClient.excluirPost(accessToken, noticia.getLinkedinPostUrn());
        } catch (Exception e) {
            log.warn("Falha ao excluir do LinkedIn o post {} da notícia {}: {}",
                    noticia.getLinkedinPostUrn(), noticia.getId(), e.getMessage());
        }
    }
}
