package br.com.gestaoloja.orcamento;

import br.com.gestaoloja.produto.Produto;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamento_item")
public class OrcamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    /* Snapshot do produto no momento do orçamento */
    @Column(name = "produto_nome", nullable = false, length = 150)
    private String produtoNome;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected OrcamentoItem() {}

    public OrcamentoItem(Produto produto, int quantidade) {
        this.produto = produto;
        this.produtoNome = produto.getNome();
        this.precoUnitario = produto.getPrecoUnitario();
        this.quantidade = quantidade;
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    void setOrcamento(Orcamento orcamento) { this.orcamento = orcamento; }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public String getProdutoNome() { return produtoNome; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}
