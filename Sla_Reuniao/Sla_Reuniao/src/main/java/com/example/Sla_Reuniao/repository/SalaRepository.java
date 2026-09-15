package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findByDisponivelTrueOrderByNomeAsc();
}