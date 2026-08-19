package com.huber.orquestrador.noticia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    boolean existsByLink(String link);

    List<Noticia> findByEstado(EstadoNoticia estado);

    Optional<Noticia> findByLink(String link);

    @Query("select n.titulo from Noticia n")
    List<String> findAllTitulos();
}
