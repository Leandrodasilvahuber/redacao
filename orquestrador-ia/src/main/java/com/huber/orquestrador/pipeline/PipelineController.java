package com.huber.orquestrador.pipeline;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/pipeline")
public class PipelineController {

    private final BuscadorService buscadorService;
    private final SeletorService seletorService;
    private final RedatorService redatorService;
    private final RevisorService revisorService;
    private final PublicadorService publicadorService;

    public PipelineController(BuscadorService buscadorService,
                               SeletorService seletorService,
                               RedatorService redatorService,
                               RevisorService revisorService,
                               PublicadorService publicadorService) {
        this.buscadorService = buscadorService;
        this.seletorService = seletorService;
        this.redatorService = redatorService;
        this.revisorService = revisorService;
        this.publicadorService = publicadorService;
    }

    @PostMapping("/buscar")
    public Map<String, Integer> buscar() {
        return Map.of("novasNoticias", buscadorService.buscar());
    }

    @PostMapping("/selecionar")
    public Map<String, Integer> selecionar() {
        return Map.of("selecionadas", seletorService.selecionar());
    }

    @PostMapping("/redigir")
    public Map<String, Integer> redigir() {
        return Map.of("redigidas", redatorService.redigir());
    }

    @PostMapping("/revisar")
    public Map<String, Integer> revisar() {
        return Map.of("revisadas", revisorService.revisar());
    }

    @PostMapping("/publicar")
    public Map<String, Integer> publicar() {
        return Map.of("prontasParaPublicar", publicadorService.publicar());
    }

    @PostMapping("/executar-tudo")
    public Map<String, Integer> executarTudo() {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("novasNoticias", buscadorService.buscar());
        resultado.put("selecionadas", seletorService.selecionar());
        resultado.put("redigidas", redatorService.redigir());
        resultado.put("revisadas", revisorService.revisar());
        resultado.put("prontasParaPublicar", publicadorService.publicar());
        return resultado;
    }
}
