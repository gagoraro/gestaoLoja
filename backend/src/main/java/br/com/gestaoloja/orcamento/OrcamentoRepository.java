package br.com.gestaoloja.orcamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findAllByOrderByCriadoEmDescIdDesc();

    @Query("select distinct o from Orcamento o left join fetch o.itens where o.id = :id")
    Optional<Orcamento> findComItens(@Param("id") Long id);
}
