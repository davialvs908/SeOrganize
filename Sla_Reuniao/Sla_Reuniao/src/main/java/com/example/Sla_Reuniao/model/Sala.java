package com.example.Sla_Reuniao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tb_salas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome ou identificador da sala é obrigatório")
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String nome;

    @Min(value = 1, message = "A capacidade mínima deve ser de pelo menos 1 pessoa")
    private int capacidade;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(length = 255)
    private String recursos;

    @Column(length = 50)
    private String localizacao;
}