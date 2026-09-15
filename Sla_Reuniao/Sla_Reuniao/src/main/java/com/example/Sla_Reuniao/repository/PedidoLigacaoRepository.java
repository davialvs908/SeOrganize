package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.PedidoLigacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoLigacaoRepository extends JpaRepository<PedidoLigacao, Long> {

    List<PedidoLigacao> findByStatus(String status);

    List<PedidoLigacao> findByNomeColaboradorOrderByDataSolicitacaoDesc(String nomeColaborador);

    Long countByNomeColaboradorAndStatus(String nomeColaborador, String status);

    List<PedidoLigacao> findBySolicitanteIdOrderByDataSolicitacaoDesc(Long solicitanteId);

    long countBySolicitanteIdAndStatus(Long solicitanteId, String status);

    List<PedidoLigacao> findByStatusAndDataSolicitacaoAfter(String status, LocalDateTime dataSolicitacao);
}