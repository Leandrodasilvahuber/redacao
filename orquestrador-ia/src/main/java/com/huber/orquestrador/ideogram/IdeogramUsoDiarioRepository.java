package com.huber.orquestrador.ideogram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface IdeogramUsoDiarioRepository extends JpaRepository<IdeogramUsoDiario, LocalDate> {
}
