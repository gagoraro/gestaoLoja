package br.com.gestaoloja.caixa;

import br.com.gestaoloja.produto.Produto;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venda_item")
public class VendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(name = "produto_nome", nullable = false, length = 150)
    private String produtoNome;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected VendaItem() {}

    public VendaItem(Produto produto, int quantidade) {
        this.produto = produto;
        this.produtoNome = produto.getNome();
        this.precoUnitario = produto.getPrecoUnitario();
        this.quantidade = quantidade;
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    void setVenda(Venda venda) { this.venda = venda; }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public String getProdutoNome() { return produtoNome; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}
