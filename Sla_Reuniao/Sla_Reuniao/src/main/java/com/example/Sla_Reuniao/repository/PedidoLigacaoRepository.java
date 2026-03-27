package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.PedidoLigacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoLigacaoRepository extends JpaRepository<PedidoLigacao, Long> {

    // Busca para a Recepção (pega todas pendentes da empresa)
    List<PedidoLigacao> findByStatus(String status);

    //  Busca só as ligações do usuário logado (da mais recente para a mais antiga)
    List<PedidoLigacao> findByNomeColaboradorOrderByDataSolicitacaoDesc(String nomeColaborador);

    //  Conta quantas ligações do usuário logado estão PENDENTES para o contador piscar
    Long countByNomeColaboradorAndStatus(String nomeColaborador, String status);
}