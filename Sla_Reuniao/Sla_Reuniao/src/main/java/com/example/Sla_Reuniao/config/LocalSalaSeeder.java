package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/** Só no H2 local, e só se ainda não houver sala, para o portal não abrir vazio. */
@Component
@Profile("local")
@Order(3)
public class LocalSalaSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSalaSeeder.class);

    private final Environment environment;
    private final DataSource dataSource;
    private final SalaRepository salaRepository;

    public LocalSalaSeeder(Environment environment,
                           DataSource dataSource,
                           SalaRepository salaRepository) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.salaRepository = salaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!podePopular()) {
            return;
        }
        if (salaRepository.count() > 0) {
            log.info("Salas locais já existem, seed ignorado.");
            return;
        }

        List<Sala> salas = List.of(
                sala("Sala 1 Reunião", 8, "TV, HDMI, quadro branco", "1º andar"),
                sala("Sala 2 Treinamento", 16, "Projetor, webcam, Wi-Fi", "2º andar"),
                sala("Sala 3 Diretoria", 6, "TV, conferência, ar-condicionado", "3º andar")
        );
        salaRepository.saveAll(salas);
        log.info("Salas locais: {} salas de demonstração criadas (H2).", salas.size());
    }

    private Sala sala(String nome, int capacidade, String recursos, String localizacao) {
        Sala sala = new Sala();
        sala.setNome(nome);
        sala.setCapacidade(capacidade);
        sala.setRecursos(recursos);
        sala.setLocalizacao(localizacao);
        sala.setDisponivel(true);
        return sala;
    }

    private boolean podePopular() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("LocalSalaSeeder ignorado: perfil prod ativo.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            if (url == null || !url.toLowerCase().contains("jdbc:h2")) {
                log.warn("LocalSalaSeeder ignorado: datasource não é H2 ({}).", url);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("LocalSalaSeeder ignorado: não foi possível inspecionar o datasource.", ex);
            return false;
        }
    }
}
