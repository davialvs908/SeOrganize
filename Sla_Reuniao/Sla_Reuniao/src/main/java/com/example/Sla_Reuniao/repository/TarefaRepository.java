package com.example.Sla_Reuniao.repository;

import com.example.Sla_Reuniao.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByStatus(String status);

    long countByStatus(String status);

    List<Tarefa> findTop8ByOrderByDataCriacaoDesc();

    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.responsaveis r WHERE t.status = :status AND (r.id = :usuarioId OR t.solicitanteId = :usuarioId)")
    List<Tarefa> findByStatusAndUsuarioEnvolvido(@Param("status") String status,
                                                 @Param("usuarioId") Long usuarioId);

    // nome fica no parâmetro só pra não quebrar quem ainda chama passando o nome; filtrar por nome misturava homônimos
    default List<Tarefa> findByStatusAndUsuarioEnvolvido(String status, Long usuarioId, String nomeSolicitante) {
        return findByStatusAndUsuarioEnvolvido(status, usuarioId);
    }

    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.responsaveis r WHERE r.id = :usuarioId OR t.solicitanteId = :usuarioId")
    List<Tarefa> findAllByUsuarioEnvolvido(@Param("usuarioId") Long usuarioId);

    default List<Tarefa> findAllByUsuarioEnvolvido(Long usuarioId, String nomeSolicitante) {
        return findAllByUsuarioEnvolvido(usuarioId); // mesmo motivo: dono é o id, não o nome
    }
}
