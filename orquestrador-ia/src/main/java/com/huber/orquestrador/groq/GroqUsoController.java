package com.huber.orquestrador.groq;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groq")
public class GroqUsoController {

    private final GroqRateLimiter rateLimiter;

    public GroqUsoController(GroqRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/uso")
    public GroqUsoStatus uso() {
        return rateLimiter.statusAtual();
    }
}
