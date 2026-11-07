package br.com.gestaoloja.orcamento;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_nome", nullable = false, length = 150)
    private String clienteNome;

    @Column(name = "cliente_contato", length = 150)
    private String clienteContato;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private LocalDate validade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrcamentoItem> itens = new ArrayList<>();

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public void adicionarItem(OrcamentoItem item) {
        item.setOrcamento(this);
        itens.add(item);
        valorTotal = itens.stream().map(OrcamentoItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public String getClienteContato() { return clienteContato; }
    public void setClienteContato(String clienteContato) { this.clienteContato = clienteContato; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public List<OrcamentoItem> getItens() { return itens; }
}
