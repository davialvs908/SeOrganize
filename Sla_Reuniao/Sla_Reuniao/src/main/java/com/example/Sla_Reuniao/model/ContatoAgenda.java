package com.example.Sla_Reuniao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "tb_agenda_telefonica")
@Data
public class ContatoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do contacto ou empresa é obrigatório")
    @Size(max = 255)
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "O número de telefone é obrigatório para a agenda")
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String telefone;

    @Size(max = 255)
    private String departamento;

    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean ativo = true;

    @PrePersist
    void garantirAtivoNaCriacao() {
        this.ativo = true;
    }

    @Transient
    public boolean isArquivado() {
        return !ativo;
    }
}
