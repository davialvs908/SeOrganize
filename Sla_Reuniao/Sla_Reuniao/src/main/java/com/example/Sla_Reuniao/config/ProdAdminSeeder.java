package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Order(1)
public class ProdAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdAdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_NOME:Administrador}")
    private String adminNome;

    public ProdAdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL e ADMIN_PASSWORD não estão definidos. Sem isso não nasce o primeiro admin.");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (usuarioRepository.findByEmail(email) != null) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome(adminNome == null || adminNome.isBlank() ? "Administrador" : adminNome.trim());
        admin.setEmail(email);
        admin.setSenha(passwordEncoder.encode(adminPassword));
        admin.setPerfil("ADMIN");
        admin.setAtivo(true);
        usuarioRepository.save(admin);
        log.info("Admin de produção criado: {}", email);
    }
}
