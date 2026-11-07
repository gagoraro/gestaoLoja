package br.com.gestaoloja.caixa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CaixaDtos {
    private CaixaDtos() {}

    public record AbrirRequest(
            @DecimalMin(value = "0.00", message = "Troco inicial não pode ser negativo")
            @Digits(integer = 10, fraction = 2) BigDecimal valorAbertura,
            @Size(max = 500) String observacao
    ) {}

    public record FecharRequest(
            @DecimalMin(value = "0.00", message = "Valor contado não pode ser negativo")
            @Digits(integer = 10, fraction = 2) BigDecimal valorContado,
            @Size(max = 500) String observacao
    ) {}

    public record ItemRequest(
            @NotNull(message = "Produto é obrigatório") Long produtoId,
            @NotNull @Positive(message = "Quantidade deve ser maior que zero") Integer quantidade
    ) {}

    public record VendaRequest(
            @NotNull(message = "Forma de pagamento é obrigatória") FormaPagamento formaPagamento,
            @NotEmpty(message = "Adicione ao menos um item") List<@Valid ItemRequest> itens,
            @Size(max = 500) String observacao
    ) {}

    public record ItemResponse(Long produtoId, String produtoNome, BigDecimal precoUnitario, int quantidade, BigDecimal subtotal) {
        static ItemResponse de(VendaItem i) {
            return new ItemResponse(i.getProduto().getId(), i.getProdutoNome(), i.getPrecoUnitario(), i.getQuantidade(), i.getSubtotal());
        }
    }

    public record VendaResponse(Long id, FormaPagamento formaPagamento, BigDecimal valorTotal, String observacao,
                                boolean cancelada, String criadoPor, OffsetDateTime criadoEm, List<ItemResponse> itens) {
        static VendaResponse de(Venda v) {
            return new VendaResponse(v.getId(), v.getFormaPagamento(), v.getValorTotal(), v.getObservacao(),
                    v.isCancelada(), v.getCriadoPor(), v.getCriadoEm(), v.getItens().stream().map(ItemResponse::de).toList());
        }
    }

    /** Totais do caixa — vendas canceladas não entram. */
    public record Resumo(int quantidadeVendas, BigDecimal totalVendas, Map<FormaPagamento, BigDecimal> totalPorForma,
                         BigDecimal dinheiroEsperadoNaGaveta, BigDecimal diferenca) {
        static Resumo calcular(Caixa c, List<Venda> vendas) {
            Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
            for (FormaPagamento f : FormaPagamento.values()) porForma.put(f, BigDecimal.ZERO);
            BigDecimal total = BigDecimal.ZERO;
            int qtd = 0;
            for (Venda v : vendas) {
                if (v.isCancelada()) continue;
                qtd++;
                total = total.add(v.getValorTotal());
                porForma.merge(v.getFormaPagamento(), v.getValorTotal(), BigDecimal::add);
            }
            BigDecimal esperado = c.getValorAbertura().add(porForma.get(FormaPagamento.DINHEIRO));
            BigDecimal diferenca = c.getValorContado() == null ? null : c.getValorContado().subtract(esperado);
            return new Resumo(qtd, total, porForma, esperado, diferenca);
        }
    }

    public record CaixaResponse(Long id, boolean aberto, BigDecimal valorAbertura, BigDecimal valorContado, String observacao,
                                String abertoPor, String fechadoPor, OffsetDateTime abertoEm, OffsetDateTime fechadoEm,
                                Resumo resumo, List<VendaResponse> vendas) {
        static CaixaResponse de(Caixa c, List<Venda> vendas) {
            return new CaixaResponse(c.getId(), c.isAberto(), c.getValorAbertura(), c.getValorContado(), c.getObservacao(),
                    c.getAbertoPor(), c.getFechadoPor(), c.getAbertoEm(), c.getFechadoEm(),
                    Resumo.calcular(c, vendas), vendas.stream().map(VendaResponse::de).toList());
        }
    }

    public record CaixaResumo(Long id, boolean aberto, OffsetDateTime abertoEm, OffsetDateTime fechadoEm,
                              String abertoPor, long quantidadeVendas, BigDecimal totalVendas) {}
}
