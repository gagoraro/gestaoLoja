package br.com.gestaoloja.orcamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class OrcamentoDtos {
    private OrcamentoDtos() {}

    public record ItemRequest(
            @NotNull(message = "Produto é obrigatório") Long produtoId,
            @NotNull @Positive(message = "Quantidade deve ser maior que zero") Integer quantidade
    ) {}

    public record OrcamentoRequest(
            @NotBlank(message = "Nome do cliente é obrigatório") @Size(max = 150) String clienteNome,
            @Size(max = 150) String clienteContato,
            @Size(max = 1000) String observacoes,
            @NotEmpty(message = "Adicione ao menos um item") List<@Valid ItemRequest> itens
    ) {}

    public record ItemResponse(Long produtoId, String produtoNome, BigDecimal precoUnitario,
                               int quantidade, BigDecimal subtotal) {
        static ItemResponse de(OrcamentoItem i) {
            return new ItemResponse(i.getProduto().getId(), i.getProdutoNome(), i.getPrecoUnitario(),
                    i.getQuantidade(), i.getSubtotal());
        }
    }

    public record OrcamentoResumo(Long id, String clienteNome, BigDecimal valorTotal,
                                  LocalDate validade, OffsetDateTime criadoEm) {
        public static OrcamentoResumo de(Orcamento o) {
            return new OrcamentoResumo(o.getId(), o.getClienteNome(), o.getValorTotal(), o.getValidade(), o.getCriadoEm());
        }
    }

    public record OrcamentoResponse(Long id, String clienteNome, String clienteContato, String observacoes,
                                    BigDecimal valorTotal, LocalDate validade, OffsetDateTime criadoEm,
                                    List<ItemResponse> itens) {
        public static OrcamentoResponse de(Orcamento o) {
            return new OrcamentoResponse(o.getId(), o.getClienteNome(), o.getClienteContato(), o.getObservacoes(),
                    o.getValorTotal(), o.getValidade(), o.getCriadoEm(),
                    o.getItens().stream().map(ItemResponse::de).toList());
        }
    }
}
