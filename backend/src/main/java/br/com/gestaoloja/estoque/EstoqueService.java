package br.com.gestaoloja.estoque;

import br.com.gestaoloja.common.RecursoNaoEncontradoException;
import br.com.gestaoloja.common.RegraNegocioException;
import br.com.gestaoloja.produto.Produto;
import br.com.gestaoloja.produto.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoRepository produtoRepository, MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /**
     * Adiciona ou remove quantidade do estoque e registra a movimentação.
     * A linha do produto é travada (FOR UPDATE) para que duas operações simultâneas
     * não leiam o mesmo saldo e uma sobrescreva a outra.
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Long produtoId, TipoMovimentacao tipo, int quantidade, String motivo) {
        Produto produto = produtoRepository.findByIdParaAtualizacao(produtoId)
                .filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + produtoId + " não encontrado"));

        int saldoAtual = produto.getQuantidadeEstoque();
        int novoSaldo = switch (tipo) {
            case ENTRADA -> saldoAtual + quantidade;
            case SAIDA -> {
                if (quantidade > saldoAtual) {
                    throw new RegraNegocioException(
                            "Estoque insuficiente: saldo atual " + saldoAtual + ", tentativa de retirar " + quantidade);
                }
                yield saldoAtual - quantidade;
            }
        };

        produto.setQuantidadeEstoque(novoSaldo);
        return movimentacaoRepository.save(new MovimentacaoEstoque(produto, tipo, quantidade, novoSaldo, motivo));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> historico(Long produtoId) {
        return movimentacaoRepository.findTop50ByProdutoIdOrderByCriadoEmDescIdDesc(produtoId);
    }
}
