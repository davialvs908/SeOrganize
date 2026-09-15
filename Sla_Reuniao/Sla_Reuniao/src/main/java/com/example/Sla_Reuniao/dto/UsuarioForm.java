package com.example.Sla_Reuniao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioForm {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255)
    private String nome;

    @NotBlank(message = "O email/login é obrigatório")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
    private String senha;

    @NotBlank(message = "O perfil é obrigatório")
    @Pattern(regexp = "ADMIN|RECEPCAO|COLABORADOR", message = "Perfil de acesso inválido")
    private String perfil;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }
}
