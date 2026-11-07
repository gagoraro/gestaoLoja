package br.com.gestaoloja.estoque;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class EstoqueDtos {
    private EstoqueDtos() {}

    public record MovimentacaoRequest(
            @NotNull(message = "Tipo é obrigatório (ENTRADA ou SAIDA)") TipoMovimentacao tipo,
            @NotNull(message = "Quantidade é obrigatória") @Positive(message = "Quantidade deve ser maior que zero") Integer quantidade,
            @Size(max = 255) String motivo
    ) {}

    public record MovimentacaoResponse(
            Long id, Long produtoId, TipoMovimentacao tipo, int quantidade,
            int saldoApos, String motivo, OffsetDateTime criadoEm
    ) {
        public static MovimentacaoResponse de(MovimentacaoEstoque m) {
            return new MovimentacaoResponse(m.getId(), m.getProduto().getId(), m.getTipo(), m.getQuantidade(),
                    m.getSaldoApos(), m.getMotivo(), m.getCriadoEm());
        }
    }
}
