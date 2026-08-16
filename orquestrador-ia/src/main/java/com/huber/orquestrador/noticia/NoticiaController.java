package com.huber.orquestrador.noticia;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/noticias")
public class NoticiaController {

    private final NoticiaRepository noticiaRepository;

    public NoticiaController(NoticiaRepository noticiaRepository) {
        this.noticiaRepository = noticiaRepository;
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
    public Noticia marcarPublicada(@PathVariable Long id) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
        noticia.mudarEstado(EstadoNoticia.PUBLICADA);
        return noticiaRepository.save(noticia);
    }
}
