package com.huber.orquestrador.mistral;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MistralUsoDiarioRepository extends JpaRepository<MistralUsoDiario, LocalDate> {
}
