package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Agendamento;
import com.example.Sla_Reuniao.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    public void validarESalvar(Agendamento novoAgendamento) {
        LocalDateTime inicio = novoAgendamento.getDataHoraInicio();
        LocalDateTime fim = novoAgendamento.getDataHoraFim();
        LocalDateTime agora = LocalDateTime.now();


        if (inicio.isBefore(agora)) {
            throw new IllegalArgumentException("Você não pode agendar uma reunião no passado.");
        }


        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new IllegalArgumentException("O horário de término deve ser DEPOIS do horário de início.");
        }


        if (inicio.getHour() < 8 || fim.getHour() >= 18) {
            throw new IllegalArgumentException("Agendamentos permitidos apenas no horário comercial (08:00 às 18:00).");
        }


        List<Agendamento> todos = agendamentoRepository.findAll();
        boolean temChoque = todos.stream().anyMatch(existente ->
                existente.getSala().getId().equals(novoAgendamento.getSala().getId()) &&
                        inicio.isBefore(existente.getDataHoraFim()) &&
                        fim.isAfter(existente.getDataHoraInicio())
        );

        if (temChoque) {
            throw new IllegalArgumentException("Esta sala já está reservada neste horário.");
        }


        novoAgendamento.setStatus("CONFIRMADO");
        agendamentoRepository.save(novoAgendamento);
    }
}