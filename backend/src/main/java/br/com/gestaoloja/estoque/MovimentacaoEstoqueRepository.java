package br.com.gestaoloja.estoque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    List<MovimentacaoEstoque> findTop50ByProdutoIdOrderByCriadoEmDescIdDesc(Long produtoId);
}
