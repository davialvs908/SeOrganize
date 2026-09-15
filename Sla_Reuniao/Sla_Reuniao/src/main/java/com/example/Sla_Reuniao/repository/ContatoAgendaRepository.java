package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.ContatoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContatoAgendaRepository extends JpaRepository<ContatoAgenda, Long> {

    List<ContatoAgenda> findByAtivoTrueOrderByNomeAsc();
}
