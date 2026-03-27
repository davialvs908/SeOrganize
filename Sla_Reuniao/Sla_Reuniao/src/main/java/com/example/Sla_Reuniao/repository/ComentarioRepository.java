package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // Busca todo o histórico de conversa de uma única tarefa, do mais antigo pro mais novo
    List<Comentario> findByTarefaIdOrderByDataCriacaoAsc(Long tarefaId);
}