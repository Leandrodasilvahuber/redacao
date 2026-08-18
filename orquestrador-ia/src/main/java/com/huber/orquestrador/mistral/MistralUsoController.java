package com.huber.orquestrador.mistral;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mistral")
public class MistralUsoController {

    private final MistralOrcamentoLimiter orcamentoLimiter;

    public MistralUsoController(MistralOrcamentoLimiter orcamentoLimiter) {
        this.orcamentoLimiter = orcamentoLimiter;
    }

    @GetMapping("/uso")
    public MistralUsoStatus uso() {
        return orcamentoLimiter.statusAtual();
    }
}
