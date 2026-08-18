package com.huber.orquestrador.ideogram;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ideogram")
public class IdeogramUsoController {

    private final IdeogramLimiter limiter;

    public IdeogramUsoController(IdeogramLimiter limiter) {
        this.limiter = limiter;
    }

    @GetMapping("/uso")
    public IdeogramUsoStatus uso() {
        return limiter.statusAtual();
    }
}
