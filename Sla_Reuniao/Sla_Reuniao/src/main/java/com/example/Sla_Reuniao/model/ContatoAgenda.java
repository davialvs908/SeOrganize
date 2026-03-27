package com.example.Sla_Reuniao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "tb_agenda_telefonica")
@Data
public class ContatoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do contacto ou empresa é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "O número de telefone é obrigatório para a agenda")
    @Column(nullable = false)
    private String telefone;

    private String departamento;
}