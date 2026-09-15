package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(1)
public class LocalDevUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDevUserSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDevUserSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("admin@local") != null) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome("Admin local");
        admin.setEmail("admin@local");
        admin.setSenha(passwordEncoder.encode("admin1234"));
        admin.setPerfil("ADMIN");
        usuarioRepository.save(admin);
        log.info("Usuário local criado: admin@local / admin1234");
    }
}
