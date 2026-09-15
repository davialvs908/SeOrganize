package com.example.Sla_Reuniao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SalaForm {

    @NotBlank(message = "O nome ou identificador da sala é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotNull(message = "A capacidade é obrigatória")
    @Min(value = 1, message = "A capacidade mínima deve ser de pelo menos 1 pessoa")
    private Integer capacidade;

    @Size(max = 255)
    private String recursos;

    @NotBlank(message = "A localização é obrigatória")
    @Size(max = 50)
    private String localizacao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public String getRecursos() {
        return recursos;
    }

    public void setRecursos(String recursos) {
        this.recursos = recursos;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }
}
