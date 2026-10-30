package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Agendamento;
import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.repository.AgendamentoRepository;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgendamentoService - regras de reserva de salas")
class AgendamentoServiceTest {

    private static final Long SALA_ID = 1L;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private SalaService salaService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Sala salaDisponivel;
    private UsuarioSessao colaborador;

    @BeforeEach
    void setUp() {
        salaDisponivel = new Sala();
        salaDisponivel.setId(SALA_ID);
        salaDisponivel.setNome("Sala Alpha");
        salaDisponivel.setCapacidade(8);
        salaDisponivel.setDisponivel(true);

        colaborador = new UsuarioSessao(10L, "Ana Souza", "ana@empresa.com", "COLABORADOR");
    }

    /** Amanhã, para nunca cair no "agendamento no passado". */
    private LocalDateTime amanhaAs(int hora, int minuto) {
        return LocalDate.now().plusDays(1).atTime(LocalTime.of(hora, minuto));
    }

    private Agendamento reserva(LocalDateTime inicio, LocalDateTime fim) {
        Sala referencia = new Sala();
        referencia.setId(SALA_ID);
        Agendamento agendamento = new Agendamento();
        agendamento.setDataHoraInicio(inicio);
        agendamento.setDataHoraFim(fim);
        agendamento.setSala(referencia);
        return agendamento;
    }

    @Nested
    @DisplayName("Ao criar uma reserva")
    class CriarReserva {

        @Test
        @DisplayName("salva a reserva válida com o usuário da sessão como responsável")
        void salvaReservaValida() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);
            when(agendamentoRepository.existsSobreposicao(eq(SALA_ID), any(), any(), isNull())).thenReturn(false);

            Agendamento nova = reserva(amanhaAs(10, 0), amanhaAs(11, 0));
            agendamentoService.validarESalvar(nova, colaborador);

            ArgumentCaptor<Agendamento> salvo = ArgumentCaptor.forClass(Agendamento.class);
            verify(agendamentoRepository).save(salvo.capture());
            assertThat(salvo.getValue().getSolicitante()).isEqualTo("Ana Souza");
            assertThat(salvo.getValue().getSolicitanteId()).isEqualTo(10L);
            assertThat(salvo.getValue().getStatus()).isEqualTo("CONFIRMADO");
            assertThat(salvo.getValue().getSala()).isSameAs(salaDisponivel);
        }

        @Test
        @DisplayName("descarta um id enviado no formulário para não sobrescrever outra reserva")
        void descartaIdEnviado() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);
            when(agendamentoRepository.existsSobreposicao(eq(SALA_ID), any(), any(), isNull())).thenReturn(false);

            Agendamento nova = reserva(amanhaAs(9, 0), amanhaAs(10, 0));
            nova.setId(999L);
            agendamentoService.validarESalvar(nova, colaborador);

            ArgumentCaptor<Agendamento> salvo = ArgumentCaptor.forClass(Agendamento.class);
            verify(agendamentoRepository).save(salvo.capture());
            assertThat(salvo.getValue().getId()).isNull();
        }

        @Test
        @DisplayName("exige um usuário logado")
        void exigeUsuarioLogado() {
            Agendamento nova = reserva(amanhaAs(10, 0), amanhaAs(11, 0));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, null))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa reunião marcada no passado")
        void recusaPassado() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);
            LocalDateTime ontem = LocalDate.now().minusDays(1).atTime(10, 0);

            Agendamento nova = reserva(ontem, ontem.plusHours(1));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("passado");
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa término antes ou igual ao início")
        void recusaTerminoAntesDoInicio() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);

            Agendamento nova = reserva(amanhaAs(11, 0), amanhaAs(10, 0));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DEPOIS");
        }

        @Test
        @DisplayName("recusa reserva que atravessa a noite")
        void recusaPernoite() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);

            Agendamento nova = reserva(amanhaAs(17, 0), amanhaAs(9, 0).plusDays(1));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mesmo dia");
        }

        @Test
        @DisplayName("recusa horário fora do expediente (08:00 às 18:00)")
        void recusaForaDoHorarioComercial() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);

            Agendamento nova = reserva(amanhaAs(7, 0), amanhaAs(8, 30));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("horário comercial");
        }

        @Test
        @DisplayName("recusa sala que já está reservada no mesmo horário")
        void recusaConflitoDeHorario() {
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);
            when(agendamentoRepository.existsSobreposicao(eq(SALA_ID), any(), any(), isNull())).thenReturn(true);

            Agendamento nova = reserva(amanhaAs(14, 0), amanhaAs(15, 0));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("já está reservada");
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa sala arquivada ou indisponível")
        void recusaSalaIndisponivel() {
            salaDisponivel.setDisponivel(false);
            when(salaService.buscarOuFalhar(SALA_ID)).thenReturn(salaDisponivel);

            Agendamento nova = reserva(amanhaAs(10, 0), amanhaAs(11, 0));

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não está disponível");
        }

        @Test
        @DisplayName("exige que uma sala seja selecionada")
        void exigeSala() {
            Agendamento nova = reserva(amanhaAs(10, 0), amanhaAs(11, 0));
            nova.setSala(null);

            assertThatThrownBy(() -> agendamentoService.validarESalvar(nova, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Selecione uma sala");
        }
    }

    @Nested
    @DisplayName("Ao cancelar uma reserva")
    class CancelarReserva {

        private Agendamento reservaDaAna() {
            Agendamento agendamento = reserva(amanhaAs(10, 0), amanhaAs(11, 0));
            agendamento.setId(5L);
            agendamento.setSolicitanteId(10L);
            agendamento.setStatus("CONFIRMADO");
            return agendamento;
        }

        @Test
        @DisplayName("o próprio solicitante pode cancelar")
        void solicitantePodeCancelar() {
            assertThat(agendamentoService.podeCancelar(reservaDaAna(), colaborador)).isTrue();
        }

        @Test
        @DisplayName("outro colaborador não pode cancelar")
        void outroColaboradorNaoPode() {
            UsuarioSessao outro = new UsuarioSessao(20L, "Bruno", "bruno@empresa.com", "COLABORADOR");
            assertThat(agendamentoService.podeCancelar(reservaDaAna(), outro)).isFalse();
        }

        @Test
        @DisplayName("administrador e recepção podem cancelar qualquer reserva")
        void adminERecepcaoPodem() {
            UsuarioSessao admin = new UsuarioSessao(1L, "Admin", "admin@empresa.com", "ADMIN");
            UsuarioSessao recepcao = new UsuarioSessao(2L, "Carla", "carla@empresa.com", "RECEPCAO");

            assertThat(agendamentoService.podeCancelar(reservaDaAna(), admin)).isTrue();
            assertThat(agendamentoService.podeCancelar(reservaDaAna(), recepcao)).isTrue();
        }

        @Test
        @DisplayName("marca a reserva como CANCELADO")
        void marcaComoCancelado() {
            Agendamento existente = reservaDaAna();
            when(agendamentoRepository.findById(5L)).thenReturn(Optional.of(existente));

            agendamentoService.cancelar(5L, colaborador);

            assertThat(existente.getStatus()).isEqualTo("CANCELADO");
            verify(agendamentoRepository).save(existente);
        }

        @Test
        @DisplayName("lança AcessoNegadoException quando o usuário não tem permissão")
        void negaSemPermissao() {
            UsuarioSessao outro = new UsuarioSessao(20L, "Bruno", "bruno@empresa.com", "COLABORADOR");
            when(agendamentoRepository.findById(5L)).thenReturn(Optional.of(reservaDaAna()));

            assertThatThrownBy(() -> agendamentoService.cancelar(5L, outro))
                    .isInstanceOf(AcessoNegadoException.class);
            verify(agendamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("informa erro quando a reserva não existe")
        void reservaInexistente() {
            when(agendamentoRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agendamentoService.cancelar(99L, colaborador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não encontrado");
        }
    }
}
