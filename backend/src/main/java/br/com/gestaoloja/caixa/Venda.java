package br.com.gestaoloja.caixa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venda")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caixa_id")
    private Caixa caixa;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private String observacao;

    @Column(nullable = false)
    private boolean cancelada;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "criado_por")
    private String criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<VendaItem> itens = new ArrayList<>();

    protected Venda() {}

    public Venda(Caixa caixa, FormaPagamento formaPagamento, String observacao, String criadoPor) {
        this.caixa = caixa;
        this.formaPagamento = formaPagamento;
        this.observacao = observacao;
        this.criadoPor = criadoPor;
    }

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public void adicionarItem(VendaItem item) {
        item.setVenda(this);
        itens.add(item);
        valorTotal = itens.stream().map(VendaItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cancelar() {
        cancelada = true;
        canceladaEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Caixa getCaixa() { return caixa; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getObservacao() { return observacao; }
    public boolean isCancelada() { return cancelada; }
    public OffsetDateTime getCanceladaEm() { return canceladaEm; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public List<VendaItem> getItens() { return itens; }
}
