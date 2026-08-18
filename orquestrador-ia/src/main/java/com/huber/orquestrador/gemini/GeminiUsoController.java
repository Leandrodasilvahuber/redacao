package com.huber.orquestrador.gemini;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gemini")
public class GeminiUsoController {

    private final GeminiRateLimiter rateLimiter;

    public GeminiUsoController(GeminiRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/uso")
    public GeminiUsoStatus uso() {
        return rateLimiter.statusAtual();
    }
}
