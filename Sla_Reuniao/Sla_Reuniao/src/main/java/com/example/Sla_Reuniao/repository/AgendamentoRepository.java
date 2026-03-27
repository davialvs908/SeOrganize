package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {


    List<Agendamento> findByDataHoraFimAfterOrderByDataHoraInicioAsc(LocalDateTime dataAtual);
}