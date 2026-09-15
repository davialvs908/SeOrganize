package com.example.Sla_Reuniao.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                System.getenv("DATABASE_URL")
        );
        if (raw == null) {
            return;
        }

        String jdbc = raw.trim();
        if (jdbc.startsWith("postgres://") || jdbc.startsWith("postgresql://")) {
            jdbc = "jdbc:" + jdbc.replaceFirst("^postgres://", "postgresql://");
        }
        if (!jdbc.startsWith("jdbc:postgresql:")) {
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        try {
            URI uri = URI.create(jdbc.substring("jdbc:".length()));
            if (uri.getUserInfo() != null) {
                String[] partes = uri.getUserInfo().split(":", 2);
                props.put("spring.datasource.username", decode(partes[0]));
                if (partes.length > 1) {
                    props.put("spring.datasource.password", decode(partes[1]));
                }
            }
            int porta = uri.getPort() > 0 ? uri.getPort() : 5432;
            String banco = uri.getPath() == null ? "" : uri.getPath();
            jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + porta + banco;
        } catch (IllegalArgumentException ignored) {
            // se o parse falhar, segue com a URL como veio
        }

        if (!jdbc.contains("sslmode=")) {
            jdbc += (jdbc.contains("?") ? "&" : "?") + "sslmode=require";
        }
        props.put("spring.datasource.url", jdbc);
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String decode(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }
}
