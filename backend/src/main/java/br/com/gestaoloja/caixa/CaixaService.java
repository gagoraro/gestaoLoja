package br.com.gestaoloja.caixa;

import br.com.gestaoloja.caixa.CaixaDtos.*;
import br.com.gestaoloja.common.RecursoNaoEncontradoException;
import br.com.gestaoloja.common.RegraNegocioException;
import br.com.gestaoloja.common.UsuarioAtual;
import br.com.gestaoloja.estoque.EstoqueService;
import br.com.gestaoloja.estoque.TipoMovimentacao;
import br.com.gestaoloja.produto.Produto;
import br.com.gestaoloja.produto.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;

    public CaixaService(CaixaRepository caixaRepository, VendaRepository vendaRepository,
                        ProdutoRepository produtoRepository, EstoqueService estoqueService) {
        this.caixaRepository = caixaRepository;
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public Optional<CaixaResponse> atual() {
        return caixaRepository.findAberto().map(this::detalhar);
    }

    @Transactional(readOnly = true)
    public CaixaResponse buscar(Long id) {
        return detalhar(caixaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caixa " + id + " não encontrado")));
    }

    @Transactional(readOnly = true)
    public List<CaixaResumo> historico() {
        List<Caixa> caixas = caixaRepository.findTop60ByOrderByAbertoEmDescIdDesc();
        if (caixas.isEmpty()) return List.of();
        Map<Long, Object[]> totais = new HashMap<>();
        for (Object[] linha : vendaRepository.totaisPorCaixa(caixas.stream().map(Caixa::getId).toList())) {
            totais.put((Long) linha[0], linha);
        }
        return caixas.stream().map(c -> {
            Object[] t = totais.get(c.getId());
            return new CaixaResumo(c.getId(), c.isAberto(), c.getAbertoEm(), c.getFechadoEm(), c.getAbertoPor(),
                    t == null ? 0 : ((Number) t[1]).longValue(),
                    t == null ? BigDecimal.ZERO : (BigDecimal) t[2]);
        }).toList();
    }

    @Transactional
    public CaixaResponse abrir(AbrirRequest req) {
        if (caixaRepository.findAberto().isPresent()) {
            throw new RegraNegocioException("Já existe um caixa aberto. Feche-o antes de abrir outro.");
        }
        Caixa c = new Caixa();
        c.setValorAbertura(req.valorAbertura() == null ? BigDecimal.ZERO : req.valorAbertura());
        c.setObservacao(req.observacao());
        c.setAbertoPor(UsuarioAtual.identificacao());
        caixaRepository.saveAndFlush(c); // UNIQUE(aberto) barra corrida entre dois "abrir" simultâneos
        return detalhar(c);
    }

    @Transactional
    public CaixaResponse fechar(Long id, FecharRequest req) {
        Caixa c = caixaAbertoTravado();
        if (!c.getId().equals(id)) {
            throw new RegraNegocioException("O caixa " + id + " não é o caixa aberto.");
        }
        c.fechar(req.valorContado(), req.observacao(), UsuarioAtual.identificacao());
        caixaRepository.flush();
        return detalhar(c);
    }

    /** Registra a venda no caixa aberto e dá baixa no estoque (tudo ou nada). */
    @Transactional
    public VendaResponse registrarVenda(VendaRequest req) {
        Caixa caixa = caixaAbertoTravado();

        Map<Long, Integer> qtdPorProduto = new LinkedHashMap<>();
        req.itens().forEach(i -> qtdPorProduto.merge(i.produtoId(), i.quantidade(), Integer::sum));

        Venda venda = new Venda(caixa, req.formaPagamento(), req.observacao(), UsuarioAtual.identificacao());
        qtdPorProduto.forEach((produtoId, qtd) -> {
            Produto p = produtoRepository.findById(produtoId)
                    .filter(Produto::isAtivo)
                    .orElseThrow(() -> new RegraNegocioException("Produto " + produtoId + " não existe ou está inativo"));
            venda.adicionarItem(new VendaItem(p, qtd));
        });
        vendaRepository.saveAndFlush(venda);

        // se faltar estoque de qualquer item, a exceção desfaz a venda inteira
        for (VendaItem item : venda.getItens()) {
            estoqueService.movimentar(item.getProduto().getId(), TipoMovimentacao.SAIDA, item.getQuantidade(),
                    "Venda #" + venda.getId());
        }
        return VendaResponse.de(venda);
    }

    /** Cancela uma venda do caixa aberto e devolve os itens ao estoque. */
    @Transactional
    public VendaResponse cancelarVenda(Long vendaId) {
        Caixa caixa = caixaAbertoTravado();
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Venda " + vendaId + " não encontrada"));
        if (!venda.getCaixa().getId().equals(caixa.getId())) {
            throw new RegraNegocioException("Só é possível cancelar vendas do caixa aberto.");
        }
        if (venda.isCancelada()) {
            throw new RegraNegocioException("Venda já está cancelada.");
        }
        venda.cancelar();
        for (VendaItem item : venda.getItens()) {
            estoqueService.movimentar(item.getProduto().getId(), TipoMovimentacao.ENTRADA, item.getQuantidade(),
                    "Cancelamento da venda #" + venda.getId());
        }
        return VendaResponse.de(venda);
    }

    private Caixa caixaAbertoTravado() {
        return caixaRepository.findAbertoParaAtualizacao()
                .orElseThrow(() -> new RegraNegocioException("Nenhum caixa aberto. Abra o caixa para continuar."));
    }

    private CaixaResponse detalhar(Caixa c) {
        List<Venda> vendas = vendaRepository.findComItensPorCaixa(c.getId());
        vendas.forEach(v -> v.getItens().forEach(i -> i.getProduto().getId()));
        return CaixaResponse.de(c, vendas);
    }
}
