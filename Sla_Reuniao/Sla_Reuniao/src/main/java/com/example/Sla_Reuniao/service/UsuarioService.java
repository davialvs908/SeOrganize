package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.dto.UsuarioForm;
import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UsuarioService {

    private static final int SENHA_MINIMA = 8;
    private static final Set<String> PERFIS_VALIDOS = Set.of("ADMIN", "RECEPCAO", "COLABORADOR");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrar(UsuarioForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Informe os dados do usuário.");
        }
        String nome = obrigatorio(form.getNome(), "O nome é obrigatório.");
        String email = normalizarEmail(form.getEmail());
        if (email.isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        if (form.getSenha() == null || form.getSenha().length() < SENHA_MINIMA) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        String perfil = form.getPerfil() == null ? "" : form.getPerfil().trim();
        if (!PERFIS_VALIDOS.contains(perfil)) {
            throw new IllegalArgumentException("Perfil de acesso inválido.");
        }
        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(form.getSenha()));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = buscarOuFalhar(id);
        if (novaSenha == null || novaSenha.length() < SENHA_MINIMA) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        String hash = passwordEncoder.encode(novaSenha);
        int atualizados = usuarioRepository.atualizarSenha(usuario.getId(), hash);
        if (atualizados != 1) {
            throw new IllegalArgumentException("Não foi possível salvar a nova senha.");
        }
    }

    @Transactional
    public void alterarSenha(Long userId, String senhaAtual, String senhaNova) {
        alterarSenha(userId, null, senhaAtual, senhaNova);
    }

    @Transactional
    public void alterarSenha(Long userId, String email, String senhaAtual, String senhaNova) {
        Usuario usuario = null;
        if (userId != null) {
            usuario = usuarioRepository.findById(userId).orElse(null);
        }
        if (usuario == null && email != null && !email.isBlank()) {
            usuario = usuarioRepository.findByEmail(email.trim());
        }
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        if (senhaAtual == null || !passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (senhaNova == null || senhaNova.length() < SENHA_MINIMA) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        String hash = passwordEncoder.encode(senhaNova);
        int atualizados = usuarioRepository.atualizarSenha(usuario.getId(), hash);
        if (atualizados != 1) {
            throw new IllegalArgumentException("Não foi possível salvar a nova senha.");
        }
    }

    @Transactional
    public void desativar(Long id) {
        Usuario usuario = buscarOuFalhar(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    public boolean isAtivo(Long id) {
        if (id == null) {
            return false;
        }
        return usuarioRepository.findById(id)
                .map(Usuario::isAtivo)
                .orElse(false);
    }

    public boolean isAtivo(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Usuario usuario = usuarioRepository.findByEmail(email);
        return usuario != null && usuario.isAtivo();
    }

    private Usuario buscarOuFalhar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    private static String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private static String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }
}
