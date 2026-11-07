package br.com.gestaoloja.caixa;

import br.com.gestaoloja.caixa.CaixaDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    /** Caixa aberto agora (204 se não houver). */
    @GetMapping("/atual")
    public ResponseEntity<CaixaResponse> atual() {
        return service.atual().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<CaixaResumo> historico() {
        return service.historico();
    }

    @GetMapping("/{id}")
    public CaixaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public CaixaResponse abrir(@Valid @RequestBody AbrirRequest req) {
        return service.abrir(req);
    }

    @PostMapping("/{id}/fechar")
    public CaixaResponse fechar(@PathVariable Long id, @Valid @RequestBody FecharRequest req) {
        return service.fechar(id, req);
    }

    @PostMapping("/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse registrarVenda(@Valid @RequestBody VendaRequest req) {
        return service.registrarVenda(req);
    }

    @PostMapping("/vendas/{id}/cancelar")
    public VendaResponse cancelarVenda(@PathVariable Long id) {
        return service.cancelarVenda(id);
    }
}
