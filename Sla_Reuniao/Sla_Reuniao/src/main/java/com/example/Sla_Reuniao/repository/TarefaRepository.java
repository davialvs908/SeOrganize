package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {


    List<Tarefa> findByStatus(String status);

    // Busca apenas as tarefas de um Status específico onde o usuário está envolvido
    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.responsaveis r WHERE t.status = :status AND (r = :usuario OR t.solicitante = :nomeSolicitante)")
    List<Tarefa> findByStatusAndUsuarioEnvolvido(@Param("status") String status, @Param("usuario") Usuario usuario, @Param("nomeSolicitante") String nomeSolicitante);

    //  Busca todas as tarefas do usuário (para gerar os Modais de Chat sem erro)
    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.responsaveis r WHERE r = :usuario OR t.solicitante = :nomeSolicitante")
    List<Tarefa> findAllByUsuarioEnvolvido(@Param("usuario") Usuario usuario, @Param("nomeSolicitante") String nomeSolicitante);
}