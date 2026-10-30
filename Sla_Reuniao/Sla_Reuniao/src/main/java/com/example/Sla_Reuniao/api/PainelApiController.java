package com.example.Sla_Reuniao.api;

import com.example.Sla_Reuniao.model.Agendamento;
import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.AgendamentoService;
import com.example.Sla_Reuniao.service.SalaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * API somente leitura usada pelo painel React (/painel).
 * A autenticação é a mesma sessão do sistema: o AutorizacaoInterceptor
 * devolve 401 em JSON para quem não está logado.
 */
@RestController
@RequestMapping("/api/painel")
public class PainelApiController {

    private final SalaService salaService;
    private final AgendamentoService agendamentoService;
    private final Clock clock;

    @Autowired
    public PainelApiController(SalaService salaService, AgendamentoService agendamentoService) {
        this(salaService, agendamentoService, Clock.systemDefaultZone());
    }

    PainelApiController(SalaService salaService, AgendamentoService agendamentoService, Clock clock) {
        this.salaService = salaService;
        this.agendamentoService = agendamentoService;
        this.clock = clock;
    }

    public record UsuarioDTO(String nome, String perfil, String iniciais) {}

    public record ReservaDTO(Long id, Long salaId, String sala, String solicitante,
                             LocalDateTime inicio, LocalDateTime fim) {}

    public record SalaDTO(Long id, String nome, int capacidade, String recursos, String localizacao,
                          boolean ocupadaAgora, ReservaDTO reservaAtual, ReservaDTO proximaReserva) {}

    @GetMapping("/me")
    public UsuarioDTO me(HttpSession session) {
        UsuarioSessao usuario = (UsuarioSessao) session.getAttribute("usuarioLogado");
        return new UsuarioDTO(usuario.getNome(), usuario.getPerfil(), usuario.getIniciais());
    }

    @GetMapping("/ocupacao")
    public List<ReservaDTO> ocupacao() {
        return agendamentoService.listarOcupacaoAtiva().stream()
                .map(PainelApiController::toDTO)
                .toList();
    }

    @GetMapping("/salas")
    public List<SalaDTO> salas() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Agendamento> ativas = agendamentoService.listarOcupacaoAtiva();

        return salaService.listarDisponiveis().stream()
                .map(sala -> montarSala(sala, ativas, agora))
                .toList();
    }

    SalaDTO montarSala(Sala sala, List<Agendamento> ativas, LocalDateTime agora) {
        List<Agendamento> daSala = ativas.stream()
                .filter(a -> a.getSala() != null && Objects.equals(a.getSala().getId(), sala.getId()))
                .sorted(Comparator.comparing(Agendamento::getDataHoraInicio))
                .toList();

        Optional<Agendamento> atual = daSala.stream()
                .filter(a -> !a.getDataHoraInicio().isAfter(agora) && a.getDataHoraFim().isAfter(agora))
                .findFirst();

        Optional<Agendamento> proxima = daSala.stream()
                .filter(a -> a.getDataHoraInicio().isAfter(agora))
                .findFirst();

        return new SalaDTO(
                sala.getId(),
                sala.getNome(),
                sala.getCapacidade(),
                sala.getRecursos(),
                sala.getLocalizacao(),
                atual.isPresent(),
                atual.map(PainelApiController::toDTO).orElse(null),
                proxima.map(PainelApiController::toDTO).orElse(null)
        );
    }

    private static ReservaDTO toDTO(Agendamento a) {
        Sala sala = a.getSala();
        return new ReservaDTO(
                a.getId(),
                sala != null ? sala.getId() : null,
                sala != null ? sala.getNome() : null,
                a.getSolicitante(),
                a.getDataHoraInicio(),
                a.getDataHoraFim()
        );
    }
}
