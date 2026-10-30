package com.example.Sla_Reuniao.api;

import com.example.Sla_Reuniao.model.Agendamento;
import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.service.AgendamentoService;
import com.example.Sla_Reuniao.service.SalaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PainelApiController - status das salas para o painel React")
class PainelApiControllerTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 3, 10, 30);

    @Mock
    private SalaService salaService;

    @Mock
    private AgendamentoService agendamentoService;

    private PainelApiController controller() {
        Clock relogio = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);
        return new PainelApiController(salaService, agendamentoService, relogio);
    }

    private Sala sala(long id, String nome) {
        Sala sala = new Sala();
        sala.setId(id);
        sala.setNome(nome);
        sala.setCapacidade(6);
        sala.setDisponivel(true);
        return sala;
    }

    private Agendamento reserva(long id, Sala sala, LocalDateTime inicio, LocalDateTime fim) {
        Agendamento a = new Agendamento();
        a.setId(id);
        a.setSala(sala);
        a.setSolicitante("Ana Souza");
        a.setDataHoraInicio(inicio);
        a.setDataHoraFim(fim);
        return a;
    }

    @Test
    @DisplayName("marca como ocupada a sala com reunião acontecendo agora")
    void salaOcupadaAgora() {
        Sala alpha = sala(1, "Alpha");
        Agendamento emAndamento = reserva(7, alpha, AGORA.minusMinutes(30), AGORA.plusMinutes(30));
        when(salaService.listarDisponiveis()).thenReturn(List.of(alpha));
        when(agendamentoService.listarOcupacaoAtiva()).thenReturn(List.of(emAndamento));

        PainelApiController.SalaDTO dto = controller().salas().get(0);

        assertThat(dto.ocupadaAgora()).isTrue();
        assertThat(dto.reservaAtual().id()).isEqualTo(7L);
        assertThat(dto.proximaReserva()).isNull();
    }

    @Test
    @DisplayName("mostra a sala livre e a próxima reserva do dia")
    void salaLivreComProximaReserva() {
        Sala beta = sala(2, "Beta");
        Agendamento tarde = reserva(9, beta, AGORA.plusHours(4), AGORA.plusHours(5));
        Agendamento antes = reserva(8, beta, AGORA.plusHours(1), AGORA.plusHours(2));
        when(salaService.listarDisponiveis()).thenReturn(List.of(beta));
        when(agendamentoService.listarOcupacaoAtiva()).thenReturn(List.of(tarde, antes));

        PainelApiController.SalaDTO dto = controller().salas().get(0);

        assertThat(dto.ocupadaAgora()).isFalse();
        assertThat(dto.proximaReserva().id()).isEqualTo(8L);
    }

    @Test
    @DisplayName("não mistura reservas de salas diferentes")
    void naoMisturaSalas() {
        Sala alpha = sala(1, "Alpha");
        Sala beta = sala(2, "Beta");
        Agendamento daAlpha = reserva(7, alpha, AGORA.minusMinutes(10), AGORA.plusMinutes(50));
        when(salaService.listarDisponiveis()).thenReturn(List.of(alpha, beta));
        when(agendamentoService.listarOcupacaoAtiva()).thenReturn(List.of(daAlpha));

        List<PainelApiController.SalaDTO> salas = controller().salas();

        assertThat(salas.get(0).ocupadaAgora()).isTrue();
        assertThat(salas.get(1).ocupadaAgora()).isFalse();
        assertThat(salas.get(1).proximaReserva()).isNull();
    }
}
