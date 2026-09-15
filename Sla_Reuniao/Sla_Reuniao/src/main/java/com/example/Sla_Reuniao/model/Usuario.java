package com.example.Sla_Reuniao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "tb_usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "O email/login é obrigatório")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Column(nullable = false, length = 100)
    @JsonIgnore
    @ToString.Exclude
    private String senha;

    @NotBlank(message = "O perfil é obrigatório")
    @Column(nullable = false)
    private String perfil;

    @Column(nullable = false)
    private boolean ativo = true;
}