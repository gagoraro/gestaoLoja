package br.com.gestaoloja.caixa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "caixa")
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TRUE = aberto; NULL = fechado (ver comentário na migration V2). */
    private Boolean aberto = Boolean.TRUE;

    @Column(name = "valor_abertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAbertura = BigDecimal.ZERO;

    @Column(name = "valor_contado", precision = 12, scale = 2)
    private BigDecimal valorContado;

    private String observacao;

    @Column(name = "aberto_por")
    private String abertoPor;

    @Column(name = "fechado_por")
    private String fechadoPor;

    @Column(name = "aberto_em", nullable = false, updatable = false)
    private OffsetDateTime abertoEm;

    @Column(name = "fechado_em")
    private OffsetDateTime fechadoEm;

    @PrePersist
    void prePersist() {
        abertoEm = OffsetDateTime.now();
    }

    public boolean isAberto() { return Boolean.TRUE.equals(aberto); }

    public void fechar(BigDecimal valorContado, String observacao, String usuario) {
        this.aberto = null;
        this.valorContado = valorContado;
        if (observacao != null && !observacao.isBlank()) this.observacao = observacao;
        this.fechadoPor = usuario;
        this.fechadoEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public BigDecimal getValorAbertura() { return valorAbertura; }
    public void setValorAbertura(BigDecimal valorAbertura) { this.valorAbertura = valorAbertura; }
    public BigDecimal getValorContado() { return valorContado; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getAbertoPor() { return abertoPor; }
    public void setAbertoPor(String abertoPor) { this.abertoPor = abertoPor; }
    public String getFechadoPor() { return fechadoPor; }
    public OffsetDateTime getAbertoEm() { return abertoEm; }
    public OffsetDateTime getFechadoEm() { return fechadoEm; }
}
