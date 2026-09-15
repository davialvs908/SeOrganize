package com.example.Sla_Reuniao.security;

import com.example.Sla_Reuniao.model.Usuario;

import java.io.Serializable;

public class UsuarioSessao implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final String email;
    private final String perfil;

    public UsuarioSessao(Long id, String nome, String email, String perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
    }

    public static UsuarioSessao from(Usuario usuario) {
        return new UsuarioSessao(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPerfil() {
        return perfil;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(perfil);
    }

    public String getIniciais() {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        char primeira = Character.toUpperCase(partes[0].charAt(0));
        if (partes.length == 1) {
            return String.valueOf(primeira);
        }
        char ultima = Character.toUpperCase(partes[partes.length - 1].charAt(0));
        return "" + primeira + ultima;
    }
}
