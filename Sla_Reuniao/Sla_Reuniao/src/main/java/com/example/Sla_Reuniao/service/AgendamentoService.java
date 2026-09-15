package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Agendamento;
import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.repository.AgendamentoRepository;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AgendamentoService {

    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(18, 0);

    private final AgendamentoRepository agendamentoRepository;
    private final SalaService salaService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository, SalaService salaService) {
        this.agendamentoRepository = agendamentoRepository;
        this.salaService = salaService;
    }

    public Agendamento buscarOuFalhar(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
    }

    public boolean podeCancelar(Agendamento agendamento, UsuarioSessao usuario) {
        if (agendamento == null || usuario == null) {
            return false;
        }
        if (usuario.isAdmin() || "RECEPCAO".equals(usuario.getPerfil())) {
            return true;
        }
        return usuario.getId() != null
                && agendamento.getSolicitanteId() != null
                && usuario.getId().equals(agendamento.getSolicitanteId());
    }

    public void garantirCancelamento(Agendamento agendamento, UsuarioSessao usuario) {
        if (!podeCancelar(agendamento, usuario)) {
            throw new AcessoNegadoException("Você não tem permissão para cancelar esta reserva.");
        }
    }

    public List<Agendamento> listarMinhasDoDia(UsuarioSessao usuario) {
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        return agendamentoRepository.findMinhasDoDia(usuario.getId(), inicio, inicio.plusDays(1));
    }

    public List<Agendamento> listarOcupacaoAtiva() {
        return agendamentoRepository.findOcupacaoAtiva(LocalDateTime.now());
    }

    /**
     * Sem sessão não dá para saber de quem é a reserva, então o id do solicitante
     * que vier no POST é descartado.
     */
    @Transactional
    public void validarESalvar(Agendamento novoAgendamento) {
        if (novoAgendamento != null) {
            novoAgendamento.setSolicitanteId(null);
        }
        persistirReserva(novoAgendamento);
    }

    @Transactional
    public void validarESalvar(Agendamento novoAgendamento, UsuarioSessao usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException(
                    "É necessário informar a sessão do usuário (UsuarioSessao) para gravar o responsável da reserva.");
        }
        if (novoAgendamento == null) {
            throw new IllegalArgumentException("Informe os dados da reserva.");
        }
        novoAgendamento.setSolicitante(usuario.getNome());
        novoAgendamento.setSolicitanteId(usuario.getId());
        persistirReserva(novoAgendamento);
    }

    private void persistirReserva(Agendamento novoAgendamento) {
        if (novoAgendamento == null) {
            throw new IllegalArgumentException("Informe os dados da reserva.");
        }

        novoAgendamento.setId(null);
        novoAgendamento.setStatus("CONFIRMADO");

        if (novoAgendamento.getSolicitante() == null || novoAgendamento.getSolicitante().isBlank()) {
            throw new IllegalArgumentException("Informe o solicitante da reserva.");
        }

        LocalDateTime inicio = novoAgendamento.getDataHoraInicio();
        LocalDateTime fim = novoAgendamento.getDataHoraFim();
        LocalDateTime agora = LocalDateTime.now();

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe o início e o término da reunião.");
        }

        if (novoAgendamento.getSala() == null || novoAgendamento.getSala().getId() == null) {
            throw new IllegalArgumentException("Selecione uma sala.");
        }

        Sala sala = salaService.buscarOuFalhar(novoAgendamento.getSala().getId());
        if (!sala.isDisponivel()) {
            throw new IllegalArgumentException("Esta sala não está disponível para reserva.");
        }
        novoAgendamento.setSala(sala);

        if (inicio.isBefore(agora)) {
            throw new IllegalArgumentException("Você não pode agendar uma reunião no passado.");
        }

        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new IllegalArgumentException("O horário de término deve ser DEPOIS do horário de início.");
        }

        if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
            throw new IllegalArgumentException("A reserva deve começar e terminar no mesmo dia (pernoite não é permitido).");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFim = fim.toLocalTime();
        if (horaInicio.isBefore(HORARIO_ABERTURA) || horaFim.isAfter(HORARIO_FECHAMENTO)) {
            throw new IllegalArgumentException("Agendamentos permitidos apenas no horário comercial (08:00 às 18:00).");
        }

        if (agendamentoRepository.existsSobreposicao(sala.getId(), inicio, fim, null)) {
            throw new IllegalArgumentException("Esta sala já está reservada neste horário.");
        }

        agendamentoRepository.save(novoAgendamento);
    }

    @Transactional
    public void cancelar(Long id, UsuarioSessao usuario) {
        Agendamento agendamento = buscarOuFalhar(id);
        garantirCancelamento(agendamento, usuario);
        if ("CANCELADO".equals(agendamento.getStatus())) {
            return;
        }
        agendamento.setStatus("CANCELADO");
        agendamentoRepository.save(agendamento);
    }
}
