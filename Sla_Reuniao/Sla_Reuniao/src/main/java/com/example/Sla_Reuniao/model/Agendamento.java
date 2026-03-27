package com.example.Sla_Reuniao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_agendamentos")
@Data
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do solicitante é obrigatório")
    @Column(nullable = false)
    private String solicitante;

    @NotNull(message = "A data e hora de início são obrigatórias")
    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "A data e hora de término são obrigatórias")
    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @ManyToOne
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(nullable = false)
    private String status = "CONFIRMADO";
}