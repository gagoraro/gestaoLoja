package br.com.gestaoloja.orcamento;

import br.com.gestaoloja.orcamento.OrcamentoDtos.OrcamentoRequest;
import br.com.gestaoloja.orcamento.OrcamentoDtos.OrcamentoResponse;
import br.com.gestaoloja.orcamento.OrcamentoDtos.OrcamentoResumo;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService service;
    private final OrcamentoPdfService pdfService;

    public OrcamentoController(OrcamentoService service, OrcamentoPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping
    public List<OrcamentoResumo> listar() {
        return service.listar().stream().map(OrcamentoResumo::de).toList();
    }

    @GetMapping("/{id}")
    public OrcamentoResponse buscar(@PathVariable Long id) {
        return OrcamentoResponse.de(service.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@Valid @RequestBody OrcamentoRequest req) {
        // recarrega com itens para a resposta
        return OrcamentoResponse.de(service.buscar(service.criar(req).getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = pdfService.gerar(service.buscar(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orcamento-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
