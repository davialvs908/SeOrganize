package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByDataHoraFimAfterOrderByDataHoraInicioAsc(LocalDateTime dataAtual);

    @Query("""
            SELECT a FROM Agendamento a
            JOIN FETCH a.sala
            WHERE a.dataHoraFim > :dataAtual
              AND a.status <> 'CANCELADO'
            ORDER BY a.dataHoraInicio ASC
            """)
    List<Agendamento> findOcupacaoAtiva(@Param("dataAtual") LocalDateTime dataAtual);

    List<Agendamento> findBySolicitanteIdAndDataHoraInicioBetween(
            Long solicitanteId, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.solicitanteId = :solicitanteId
              AND a.status <> 'CANCELADO'
              AND a.dataHoraInicio >= :inicio
              AND a.dataHoraInicio < :fim
            ORDER BY a.dataHoraInicio ASC
            """)
    List<Agendamento> findMinhasDoDia(@Param("solicitanteId") Long solicitanteId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COUNT(a) > 0 FROM Agendamento a
            WHERE a.sala.id = :salaId
              AND a.status <> 'CANCELADO'
              AND a.dataHoraInicio < :fim
              AND a.dataHoraFim > :inicio
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsSobreposicao(@Param("salaId") Long salaId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim,
                               @Param("excludeId") Long excludeId);

    default boolean existsSobreposicao(Long salaId, LocalDateTime inicio, LocalDateTime fim) {
        return existsSobreposicao(salaId, inicio, fim, null);
    }
}
