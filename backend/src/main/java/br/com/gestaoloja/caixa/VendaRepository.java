package br.com.gestaoloja.caixa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query("select distinct v from Venda v left join fetch v.itens where v.caixa.id = :caixaId order by v.id desc")
    List<Venda> findComItensPorCaixa(@Param("caixaId") Long caixaId);

    @Query("select v.caixa.id, count(v), coalesce(sum(v.valorTotal), 0) from Venda v "
            + "where v.cancelada = false and v.caixa.id in :ids group by v.caixa.id")
    List<Object[]> totaisPorCaixa(@Param("ids") List<Long> ids);
}
