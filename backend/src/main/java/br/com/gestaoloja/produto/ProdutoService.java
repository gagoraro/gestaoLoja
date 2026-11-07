package br.com.gestaoloja.produto;

import br.com.gestaoloja.common.RecursoNaoEncontradoException;
import br.com.gestaoloja.common.RegraNegocioException;
import br.com.gestaoloja.estoque.EstoqueService;
import br.com.gestaoloja.estoque.TipoMovimentacao;
import br.com.gestaoloja.produto.ProdutoDtos.ProdutoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        String sku = limpar(req.sku());
        if (sku != null && repository.existsBySkuIgnoreCase(sku)) {
            throw new RegraNegocioException("Já existe um produto com o SKU " + sku);
        }
        Produto p = new Produto();
        aplicar(p, req);
        // sem SKU: grava com um código provisório único e troca pelo definitivo baseado no id
        p.setSku(sku != null ? sku : "TMP-" + UUID.randomUUID());
        p.setQuantidadeEstoque(0);
        p = repository.saveAndFlush(p);
        if (sku == null) {
            p.setSku(gerarSku(p.getId()));
        }

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
        String sku = limpar(req.sku());
        if (sku != null) { // vazio na edição = mantém o SKU atual
            if (repository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
                throw new RegraNegocioException("Já existe um produto com o SKU " + sku);
            }
            p.setSku(sku);
        }
        aplicar(p, req);
        return p;
    }

    /** Exclusão lógica: preserva histórico de estoque e orçamentos que referenciam o produto. */
    @Transactional
    public void remover(Long id) {
        buscar(id).setAtivo(false);
    }

    /** P + id com 5 dígitos; se alguém já usou esse código manualmente, acrescenta um sufixo. */
    private String gerarSku(Long id) {
        String base = String.format("P%05d", id);
        String sku = base;
        for (int i = 2; repository.existsBySkuIgnoreCaseAndIdNot(sku, id); i++) {
            sku = base + "-" + i;
        }
        return sku;
    }

    private static String limpar(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim();
    }

    private void aplicar(Produto p, ProdutoRequest req) {
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao());
        p.setPrecoUnitario(req.precoUnitario());
    }
}
