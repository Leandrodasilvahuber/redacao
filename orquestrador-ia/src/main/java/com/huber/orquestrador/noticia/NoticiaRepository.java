package com.huber.orquestrador.noticia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    boolean existsByLink(String link);

    List<Noticia> findByEstado(EstadoNoticia estado);

    Optional<Noticia> findByLink(String link);
}
