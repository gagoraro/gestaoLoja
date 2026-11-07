package br.com.gestaoloja.produto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ProdutoDtos {
    private ProdutoDtos() {}

    public record ProdutoRequest(
            @NotBlank(message = "SKU é obrigatório") @Size(max = 50) String sku,
            @NotBlank(message = "Nome é obrigatório") @Size(max = 150) String nome,
            @Size(max = 1000) String descricao,
            @NotNull(message = "Preço é obrigatório") @DecimalMin(value = "0.00", message = "Preço não pode ser negativo")
            @Digits(integer = 10, fraction = 2) BigDecimal precoUnitario,
            /* só usado na criação: estoque inicial */
            @PositiveOrZero(message = "Estoque inicial não pode ser negativo") Integer estoqueInicial
    ) {}

    public record ProdutoResponse(
            Long id, String sku, String nome, String descricao,
            BigDecimal precoUnitario, int quantidadeEstoque,
            OffsetDateTime criadoEm, OffsetDateTime atualizadoEm
    ) {
        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getSku(), p.getNome(), p.getDescricao(),
                    p.getPrecoUnitario(), p.getQuantidadeEstoque(), p.getCriadoEm(), p.getAtualizadoEm());
        }
    }
}
