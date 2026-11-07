package br.com.gestaoloja.produto;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByNome();

    // obs.: não usar ":param is null" aqui — no Postgres um null sem tipo vira bytea e quebra o lower()
    @Query("""
            select p from Produto p
            where p.ativo = true
              and (lower(p.nome) like lower(concat('%', :busca, '%'))
                   or lower(p.sku) like lower(concat('%', :busca, '%')))
            order by p.nome
            """)
    List<Produto> buscarAtivos(@Param("busca") String busca);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySkuIgnoreCase(String sku);

    /** Trava a linha (SELECT ... FOR UPDATE) para evitar corrida em movimentações simultâneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> findByIdParaAtualizacao(@Param("id") Long id);
}
