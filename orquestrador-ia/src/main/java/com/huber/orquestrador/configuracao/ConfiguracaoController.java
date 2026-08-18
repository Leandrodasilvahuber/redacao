package com.huber.orquestrador.configuracao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configuracoes")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    @GetMapping
    public ConfiguracaoDTO.Response obter() {
        return configuracaoService.obterResposta();
    }

    @PutMapping
    public ConfiguracaoDTO.Response salvar(@RequestBody ConfiguracaoDTO.Request request) {
        return configuracaoService.salvar(request);
    }
}
