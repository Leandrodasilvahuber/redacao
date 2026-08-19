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

    /**
     * Publica de novo com a imagem nova e só então exclui o post anterior (se houver). A API do
     * LinkedIn não permite editar a imagem de um post já publicado, então pra trocar a capa é
     * preciso publicar outro e excluir o antigo — usado pra atualizar posts antigos pro padrão
     * visual atual. Publica antes de excluir de propósito: se a publicação nova falhar, o post
     * antigo continua no ar em vez de a notícia ficar sem nenhum post publicado. Ao contrário de
     * {@link #publicar}, deixa a exceção subir: aqui é uma ação manual e pontual, então o chamador
     * precisa saber na hora se falhou, em vez de só logar.
     */
    public void republicarComNovaImagem(Noticia noticia, byte[] imagemPng) {
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

        String postUrnAnterior = noticia.getLinkedinPostUrn();
        String postUrn = linkedInClient.publicarPost(accessToken, personUrn, noticia.getTextoFinal(), imagemPng);
        noticia.marcarPublicadaNoLinkedin(postUrn);

        if (postUrnAnterior != null && !postUrnAnterior.isBlank()) {
            try {
                linkedInClient.excluirPost(accessToken, postUrnAnterior);
            } catch (Exception e) {
                log.warn("Post novo publicado, mas falha ao excluir o post anterior {} do LinkedIn da notícia {}: {}",
                        postUrnAnterior, noticia.getId(), e.getMessage());
            }
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
