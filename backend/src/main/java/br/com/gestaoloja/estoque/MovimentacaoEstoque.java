package br.com.gestaoloja.estoque;

import br.com.gestaoloja.produto.Produto;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "saldo_apos", nullable = false)
    private int saldoApos;

    private String motivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected MovimentacaoEstoque() {}

    public MovimentacaoEstoque(Produto produto, TipoMovimentacao tipo, int quantidade, int saldoApos, String motivo) {
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.motivo = motivo;
    }

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public TipoMovimentacao getTipo() { return tipo; }
    public int getQuantidade() { return quantidade; }
    public int getSaldoApos() { return saldoApos; }
    public String getMotivo() { return motivo; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
