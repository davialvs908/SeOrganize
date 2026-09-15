package com.example.Sla_Reuniao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContatoAgendaForm {

    @NotBlank(message = "O nome do contacto ou empresa é obrigatório")
    @Size(max = 255)
    private String nome;

    @NotBlank(message = "O número de telefone é obrigatório para a agenda")
    @Size(max = 40)
    private String telefone;

    @Size(max = 255)
    private String departamento;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
}
