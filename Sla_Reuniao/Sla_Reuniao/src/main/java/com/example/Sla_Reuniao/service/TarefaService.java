package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.repository.TarefaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TarefaService {

    private final TarefaRepository tarefaRepository;

    public TarefaService(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }


    public void moverTarefa(Long id, String novoStatus) {
        Optional<Tarefa> tarefaOpt = tarefaRepository.findById(id);
        if (tarefaOpt.isPresent()) {
            Tarefa tarefa = tarefaOpt.get();
            tarefa.setStatus(novoStatus);
            tarefaRepository.save(tarefa);
        }
    }
}