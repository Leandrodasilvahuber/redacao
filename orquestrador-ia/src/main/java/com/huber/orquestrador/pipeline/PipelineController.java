package com.huber.orquestrador.pipeline;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final IlustradorService ilustradorService;
    private final PublicadorService publicadorService;

    public PipelineController(BuscadorService buscadorService,
                               SeletorService seletorService,
                               RedatorService redatorService,
                               RevisorService revisorService,
                               IlustradorService ilustradorService,
                               PublicadorService publicadorService) {
        this.buscadorService = buscadorService;
        this.seletorService = seletorService;
        this.redatorService = redatorService;
        this.revisorService = revisorService;
        this.ilustradorService = ilustradorService;
        this.publicadorService = publicadorService;
    }

    @PostMapping("/buscar")
    public Map<String, Integer> buscar() {
        return Map.of("novasNoticias", buscadorService.buscar());
    }

    @PostMapping("/selecionar")
    public Map<String, Integer> selecionar(@RequestParam(required = false) Long id) {
        return Map.of("selecionadas", seletorService.selecionar(id));
    }

    @PostMapping("/redigir")
    public Map<String, Integer> redigir(@RequestParam(required = false) Long id) {
        return Map.of("redigidas", redatorService.redigir(id));
    }

    @PostMapping("/revisar")
    public Map<String, Integer> revisar(@RequestParam(required = false) Long id) {
        return Map.of("revisadas", revisorService.revisar(id));
    }

    @PostMapping("/ilustrar")
    public Map<String, Integer> ilustrar(@RequestParam(required = false) Long id) {
        return Map.of("ilustradas", ilustradorService.ilustrar(id));
    }

    @PostMapping("/publicar")
    public Map<String, Integer> publicar(@RequestParam(required = false) Long id) {
        return Map.of("prontasParaPublicar", publicadorService.publicar(id));
    }

    @PostMapping("/executar-tudo")
    public Map<String, Integer> executarTudo(@RequestParam(required = false) Long id) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        if (id != null) {
            resultado.put("selecionadas", seletorService.selecionar(id));
            resultado.put("redigidas", redatorService.redigir(id));
            resultado.put("revisadas", revisorService.revisar(id));
            resultado.put("ilustradas", ilustradorService.ilustrar(id));
            resultado.put("prontasParaPublicar", publicadorService.publicar(id));
            return resultado;
        }
        resultado.put("novasNoticias", buscadorService.buscar());
        resultado.put("selecionadas", seletorService.selecionar());
        resultado.put("redigidas", redatorService.redigir());
        resultado.put("revisadas", revisorService.revisar());
        resultado.put("ilustradas", ilustradorService.ilustrar());
        resultado.put("prontasParaPublicar", publicadorService.publicar());
        return resultado;
    }
}
