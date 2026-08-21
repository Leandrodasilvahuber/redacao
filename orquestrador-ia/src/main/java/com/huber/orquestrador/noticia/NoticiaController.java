package com.huber.orquestrador.noticia;

import com.huber.orquestrador.blog.BlogPublicadorService;
import com.huber.orquestrador.linkedin.LinkedInPublicadorService;
import com.huber.orquestrador.mistral.LimiteMistralAtingidoException;
import com.huber.orquestrador.pipeline.CriadorManualService;
import com.huber.orquestrador.pipeline.IlustradorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/noticias")
public class NoticiaController {

    private final NoticiaRepository noticiaRepository;
    private final LinkedInPublicadorService linkedInPublicadorService;
    private final BlogPublicadorService blogPublicadorService;
    private final IlustradorService ilustradorService;
    private final CriadorManualService criadorManualService;

    public NoticiaController(NoticiaRepository noticiaRepository,
                              LinkedInPublicadorService linkedInPublicadorService,
                              BlogPublicadorService blogPublicadorService,
                              IlustradorService ilustradorService,
                              CriadorManualService criadorManualService) {
        this.noticiaRepository = noticiaRepository;
        this.linkedInPublicadorService = linkedInPublicadorService;
        this.blogPublicadorService = blogPublicadorService;
        this.ilustradorService = ilustradorService;
        this.criadorManualService = criadorManualService;
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

        byte[] imagemPng = body != null ? decodificarPng(body.imagemPngBase64()) : null;

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

    /**
     * Gera de novo só o ícone da capa (mantendo a cor de destaque atual), no lugar da capa inteira.
     * {@code descricao} é opcional e, quando informada, guia a IA na escolha do ícone.
     */
    @PostMapping("/{id}/regerar-icone")
    public RegerarCapaResponse regerarIcone(@PathVariable Long id,
                                             @RequestBody(required = false) RegerarIconeRequest body) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        String descricao = body != null ? body.descricao() : null;
        return new RegerarCapaResponse(ilustradorService.regerarIcone(noticia, descricao));
    }

    public record RegerarIconeRequest(String descricao) {
    }

    /** Reenvia a capa (já rasterizada em PNG) pro post existente no blog. */
    @PutMapping("/{id}/capa-blog")
    public Noticia atualizarCapaBlog(@PathVariable Long id, @RequestBody ImagemBase64Request body) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        byte[] imagem = exigirImagem(body);
        try {
            blogPublicadorService.atualizarCapa(noticia, imagem);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao atualizar a capa no blog: " + e.getMessage());
        }
        return noticia;
    }

    /**
     * Exclui o post atual do LinkedIn e publica de novo com a capa nova (a API do LinkedIn não
     * permite editar a imagem de um post existente).
     */
    @PutMapping("/{id}/capa-linkedin")
    public Noticia atualizarCapaLinkedin(@PathVariable Long id, @RequestBody ImagemBase64Request body) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        byte[] imagem = exigirImagem(body);
        try {
            linkedInPublicadorService.republicarComNovaImagem(noticia, imagem);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao republicar no LinkedIn: " + e.getMessage());
        }
        return noticiaRepository.save(noticia);
    }

    public record ImagemBase64Request(String imagemPngBase64) {
    }

    private static byte[] exigirImagem(ImagemBase64Request body) {
        if (body == null || body.imagemPngBase64() == null || body.imagemPngBase64().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imagemPngBase64 é obrigatório");
        }
        return decodificarPng(body.imagemPngBase64());
    }

    private static byte[] decodificarPng(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        int virgula = base64.indexOf(',');
        String conteudo = virgula >= 0 ? base64.substring(virgula + 1) : base64;
        return Base64.getDecoder().decode(conteudo);
    }

    /** Pede ao Mistral pra formatar um texto colado em notícia ou breve tutorial, sem salvar nada. */
    @PostMapping("/formatar-manual")
    public FormatoManualResponse formatarManual(@RequestBody FormatarManualRequest body) {
        if (body == null || body.texto() == null || body.texto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "texto é obrigatório");
        }
        CriadorManualService.TipoFormatacao tipo = tipoFormatacao(body.tipo());
        try {
            CriadorManualService.FormatoGerado gerado = criadorManualService.formatar(body.texto(), tipo);
            return new FormatoManualResponse(gerado.titulo(), gerado.texto());
        } catch (LimiteMistralAtingidoException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Limite do Mistral atingido, tente de novo mais tarde: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao formatar o texto: " + e.getMessage());
        }
    }

    private static CriadorManualService.TipoFormatacao tipoFormatacao(String tipo) {
        try {
            return tipo != null
                    ? CriadorManualService.TipoFormatacao.valueOf(tipo)
                    : CriadorManualService.TipoFormatacao.NOTICIA;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo inválido: " + tipo);
        }
    }

    public record FormatarManualRequest(String texto, String tipo) {
    }

    public record FormatoManualResponse(String titulo, String texto) {
    }

    /** Salva a notícia criada manualmente (já formatada) no pipeline, no estado REVISADA. */
    @PostMapping("/criar-manual")
    public Noticia criarManual(@RequestBody CriarManualRequest body) {
        if (body == null || body.titulo() == null || body.titulo().isBlank()
                || body.texto() == null || body.texto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titulo e texto são obrigatórios");
        }
        return criadorManualService.salvar(body.titulo(), body.texto());
    }

    public record CriarManualRequest(String titulo, String texto) {
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

    /**
     * Exclui de uma vez todas as notícias de um estado do pipeline (ex.: limpar a coluna
     * "Buscada"). Não é permitido em notícias já PUBLICADAs, pra evitar apagar em massa posts que
     * já foram ao ar.
     */
    @DeleteMapping
    public Map<String, Integer> excluirTodas(@RequestParam EstadoNoticia estado) {
        if (estado == EstadoNoticia.PUBLICADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível excluir em massa notícias já publicadas");
        }
        List<Noticia> noticias = noticiaRepository.findByEstado(estado);
        for (Noticia noticia : noticias) {
            linkedInPublicadorService.excluir(noticia);
            blogPublicadorService.excluir(noticia);
        }
        noticiaRepository.deleteAll(noticias);
        return Map.of("excluidas", noticias.size());
    }
}
