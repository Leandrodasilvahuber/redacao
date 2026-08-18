package com.huber.orquestrador.groq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface GroqUsoDiarioRepository extends JpaRepository<GroqUsoDiario, LocalDate> {
}
