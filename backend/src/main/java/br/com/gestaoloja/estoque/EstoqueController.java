package br.com.gestaoloja.estoque;

import br.com.gestaoloja.estoque.EstoqueDtos.MovimentacaoRequest;
import br.com.gestaoloja.estoque.EstoqueDtos.MovimentacaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos/{produtoId}/movimentacoes")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse movimentar(@PathVariable Long produtoId, @Valid @RequestBody MovimentacaoRequest req) {
        return MovimentacaoResponse.de(service.movimentar(produtoId, req.tipo(), req.quantidade(), req.motivo()));
    }

    @GetMapping
    public List<MovimentacaoResponse> historico(@PathVariable Long produtoId) {
        return service.historico(produtoId).stream().map(MovimentacaoResponse::de).toList();
    }
}
