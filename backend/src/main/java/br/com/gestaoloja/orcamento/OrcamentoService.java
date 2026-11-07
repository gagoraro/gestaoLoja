package br.com.gestaoloja.orcamento;

import br.com.gestaoloja.common.RecursoNaoEncontradoException;
import br.com.gestaoloja.common.RegraNegocioException;
import br.com.gestaoloja.config.AppProperties;
import br.com.gestaoloja.orcamento.OrcamentoDtos.ItemRequest;
import br.com.gestaoloja.orcamento.OrcamentoDtos.OrcamentoRequest;
import br.com.gestaoloja.produto.Produto;
import br.com.gestaoloja.produto.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrcamentoService {

    private final OrcamentoRepository repository;
    private final ProdutoRepository produtoRepository;
    private final AppProperties props;

    public OrcamentoService(OrcamentoRepository repository, ProdutoRepository produtoRepository, AppProperties props) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<Orcamento> listar() {
        return repository.findAllByOrderByCriadoEmDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Orcamento buscar(Long id) {
        Orcamento o = repository.findComItens(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento " + id + " não encontrado"));
        o.getItens().forEach(i -> i.getProduto().getId()); // garante dados carregados fora da transação
        return o;
    }

    /**
     * Cria o orçamento. Preços vêm SEMPRE do cadastro do produto (nunca do front),
     * e o total é calculado no servidor. Orçamento não baixa estoque.
     */
    @Transactional
    public Orcamento criar(OrcamentoRequest req) {
        // agrupa itens repetidos do mesmo produto
        Map<Long, Integer> qtdPorProduto = new LinkedHashMap<>();
        for (ItemRequest item : req.itens()) {
            qtdPorProduto.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        Orcamento o = new Orcamento();
        o.setClienteNome(req.clienteNome().trim());
        o.setClienteContato(req.clienteContato());
        o.setObservacoes(req.observacoes());
        o.setValidade(LocalDate.now().plusDays(props.loja().validadeOrcamentoDias()));

        qtdPorProduto.forEach((produtoId, qtd) -> {
            Produto p = produtoRepository.findById(produtoId)
                    .filter(Produto::isAtivo)
                    .orElseThrow(() -> new RegraNegocioException("Produto " + produtoId + " não existe ou está inativo"));
            o.adicionarItem(new OrcamentoItem(p, qtd));
        });

        return repository.save(o);
    }

    @Transactional
    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Orçamento " + id + " não encontrado");
        }
        repository.deleteById(id);
    }
}
