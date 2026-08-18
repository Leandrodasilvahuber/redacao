package com.huber.orquestrador.gemini;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface GeminiUsoDiarioRepository extends JpaRepository<GeminiUsoDiario, LocalDate> {
}
