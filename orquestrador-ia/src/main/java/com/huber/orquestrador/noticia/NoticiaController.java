package com.huber.orquestrador.noticia;

import com.huber.orquestrador.blog.BlogPublicadorService;
import com.huber.orquestrador.linkedin.LinkedInPublicadorService;
import com.huber.orquestrador.pipeline.IlustradorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/noticias")
public class NoticiaController {

    private final NoticiaRepository noticiaRepository;
    private final LinkedInPublicadorService linkedInPublicadorService;
    private final BlogPublicadorService blogPublicadorService;
    private final IlustradorService ilustradorService;

    public NoticiaController(NoticiaRepository noticiaRepository,
                              LinkedInPublicadorService linkedInPublicadorService,
                              BlogPublicadorService blogPublicadorService,
                              IlustradorService ilustradorService) {
        this.noticiaRepository = noticiaRepository;
        this.linkedInPublicadorService = linkedInPublicadorService;
        this.blogPublicadorService = blogPublicadorService;
        this.ilustradorService = ilustradorService;
    }

    @GetMapping
    public List<Noticia> listar() {
        return noticiaRepository.findAll();
    }

    @GetMapping("/prontas")
    public List<Noticia> listarProntas() {
        return noticiaRepository.findByEstado(EstadoNoticia.PRONTA_PARA_PUBLICAR);
    }

    @PostMapping("/{id}/marcar-publicada")
    public Noticia marcarPublicada(@PathVariable Long id,
                                    @RequestBody(required = false) MarcarPublicadaRequest body) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));

        byte[] imagemPng = null;
        if (body != null && body.imagemPngBase64() != null && !body.imagemPngBase64().isBlank()) {
            String base64 = body.imagemPngBase64();
            int virgula = base64.indexOf(',');
            if (virgula >= 0) {
                base64 = base64.substring(virgula + 1);
            }
            imagemPng = Base64.getDecoder().decode(base64);
        }

        linkedInPublicadorService.publicar(noticia, imagemPng);
        blogPublicadorService.publicar(noticia, imagemPng);

        noticia.mudarEstado(EstadoNoticia.PUBLICADA);
        return noticiaRepository.save(noticia);
    }

    public record MarcarPublicadaRequest(String imagemPngBase64) {
    }

    /**
     * Gera de novo a capa da notícia no padrão visual atual (não publica nada sozinho — o
     * front-end rasteriza o SVG devolvido e manda pra {@link #atualizarCapaBlog}).
     */
    @PostMapping("/{id}/regerar-capa")
    public RegerarCapaResponse regerarCapa(@PathVariable Long id) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        return new RegerarCapaResponse(ilustradorService.regerarIlustracao(noticia));
    }

    public record RegerarCapaResponse(String svgIlustracao) {
    }

    /** Reenvia a capa (já rasterizada em PNG) pro post existente no blog. */
    @PutMapping("/{id}/capa-blog")
    public Noticia atualizarCapaBlog(@PathVariable Long id, @RequestBody AtualizarCapaBlogRequest body) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        if (body == null || body.imagemPngBase64() == null || body.imagemPngBase64().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imagemPngBase64 é obrigatório");
        }
        String base64 = body.imagemPngBase64();
        int virgula = base64.indexOf(',');
        if (virgula >= 0) {
            base64 = base64.substring(virgula + 1);
        }
        try {
            blogPublicadorService.atualizarCapa(noticia, Base64.getDecoder().decode(base64));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao atualizar a capa no blog: " + e.getMessage());
        }
        return noticia;
    }

    public record AtualizarCapaBlogRequest(String imagemPngBase64) {
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        linkedInPublicadorService.excluir(noticia);
        blogPublicadorService.excluir(noticia);
        noticiaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
