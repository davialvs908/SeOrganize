package com.example.Sla_Reuniao.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AgendamentoForm {

    @NotNull(message = "A data e hora de início são obrigatórias")
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "A data e hora de término são obrigatórias")
    private LocalDateTime dataHoraFim;

    @NotNull(message = "A sala é obrigatória")
    private Long salaId;

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }
}
