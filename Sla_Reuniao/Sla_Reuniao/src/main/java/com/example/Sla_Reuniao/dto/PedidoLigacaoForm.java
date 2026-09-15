package com.example.Sla_Reuniao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PedidoLigacaoForm {

    @NotBlank(message = "Para quem a recepção deve ligar?")
    @Size(max = 255)
    private String destinatario;

    @Size(max = 40)
    private String telefone;

    private Long contatoAgendaId;

    @NotBlank(message = "O motivo da ligação é obrigatório")
    @Size(max = 500)
    private String motivoLigacao;

    @NotBlank(message = "A urgência é obrigatória")
    @Pattern(regexp = "BAIXA|NORMAL|ALTA", message = "Urgência inválida")
    private String urgencia = "NORMAL";

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Long getContatoAgendaId() {
        return contatoAgendaId;
    }

    public void setContatoAgendaId(Long contatoAgendaId) {
        this.contatoAgendaId = contatoAgendaId;
    }

    public String getMotivoLigacao() {
        return motivoLigacao;
    }

    public void setMotivoLigacao(String motivoLigacao) {
        this.motivoLigacao = motivoLigacao;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }
}
