package br.com.gestaoloja.caixa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CaixaRepository extends JpaRepository<Caixa, Long> {

    @Query("select c from Caixa c where c.aberto = true")
    Optional<Caixa> findAberto();

    /** Trava o caixa aberto: vendas e fechamento não acontecem ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.aberto = true")
    Optional<Caixa> findAbertoParaAtualizacao();

    List<Caixa> findTop60ByOrderByAbertoEmDescIdDesc();
}
