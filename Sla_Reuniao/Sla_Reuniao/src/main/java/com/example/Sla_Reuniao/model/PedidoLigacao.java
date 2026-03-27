package com.example.Sla_Reuniao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pedidos_ligacao")
@Data
public class PedidoLigacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do colaborador solicitante é obrigatório")
    @Column(nullable = false)
    private String nomeColaborador;

    @NotBlank(message = "Para quem a recepção deve ligar?")
    @Column(nullable = false)
    private String destinatario;


    @Column(nullable = true)
    private String telefone;


    @ManyToOne
    @JoinColumn(name = "contato_id", nullable = true)
    private ContatoAgenda contatoAgenda;

    @Column(length = 500)
    private String motivoLigacao;

    @Column(nullable = false)
    private String urgencia = "NORMAL";

    @Column(nullable = false)
    private String status = "PENDENTE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao = LocalDateTime.now();
}