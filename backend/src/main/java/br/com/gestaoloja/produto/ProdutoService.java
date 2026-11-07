package br.com.gestaoloja.produto;

import br.com.gestaoloja.common.RecursoNaoEncontradoException;
import br.com.gestaoloja.common.RegraNegocioException;
import br.com.gestaoloja.estoque.EstoqueService;
import br.com.gestaoloja.estoque.TipoMovimentacao;
import br.com.gestaoloja.produto.ProdutoDtos.ProdutoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final EstoqueService estoqueService;

    public ProdutoService(ProdutoRepository repository, EstoqueService estoqueService) {
        this.repository = repository;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar(String busca) {
        return busca == null || busca.isBlank()
                ? repository.findByAtivoTrueOrderByNome()
                : repository.buscarAtivos(busca.trim());
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return repository.findById(id)
                .filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado"));
    }

    @Transactional
    public Produto criar(ProdutoRequest req) {
        String sku = req.sku().trim();
        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new RegraNegocioException("Já existe um produto com o SKU " + sku);
        }
        Produto p = new Produto();
        aplicar(p, req);
        p.setQuantidadeEstoque(0);
        p = repository.save(p);

        int inicial = req.estoqueInicial() == null ? 0 : req.estoqueInicial();
        if (inicial > 0) {
            // estoque inicial também gera histórico
            estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, inicial, "Estoque inicial");
        }
        return p;
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoRequest req) {
        Produto p = buscar(id);
        if (repository.existsBySkuIgnoreCaseAndIdNot(req.sku().trim(), id)) {
            throw new RegraNegocioException("Já existe um produto com o SKU " + req.sku().trim());
        }
        aplicar(p, req);
        return p;
    }

    /** Exclusão lógica: preserva histórico de estoque e orçamentos que referenciam o produto. */
    @Transactional
    public void remover(Long id) {
        buscar(id).setAtivo(false);
    }

    private void aplicar(Produto p, ProdutoRequest req) {
        p.setSku(req.sku().trim());
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao());
        p.setPrecoUnitario(req.precoUnitario());
    }
}
