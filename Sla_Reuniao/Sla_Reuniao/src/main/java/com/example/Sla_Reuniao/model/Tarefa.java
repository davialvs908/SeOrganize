package com.example.Sla_Reuniao.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_tarefas")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;


    private String solicitante;


    @ManyToMany
    @JoinTable(
            name = "tarefa_usuarios",
            joinColumns = @JoinColumn(name = "tarefa_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> responsaveis = new ArrayList<>();

    @Column(nullable = false)
    private String status;

    private LocalDateTime dataCriacao;

    @OneToMany(mappedBy = "tarefa", cascade = CascadeType.ALL)
    @OrderBy("dataCriacao ASC")
    private List<Comentario> comentarios;

    public Tarefa() {
        this.dataCriacao = LocalDateTime.now();
        this.status = "A_FAZER";
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }


    public List<Usuario> getResponsaveis() { return responsaveis; }
    public void setResponsaveis(List<Usuario> responsaveis) { this.responsaveis = responsaveis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public List<Comentario> getComentarios() { return comentarios; }
    public void setComentarios(List<Comentario> comentarios) { this.comentarios = comentarios; }
}