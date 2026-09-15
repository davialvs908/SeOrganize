package com.example.Sla_Reuniao.security;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = TimeUnit.MINUTES.toMillis(15);

    private final ConcurrentHashMap<String, Tentativa> tentativas = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String chave) {
        limparExpirados();
        Tentativa tentativa = tentativas.get(chave);
        if (tentativa == null) {
            return false;
        }
        if (expirada(tentativa)) {
            tentativas.remove(chave);
            return false;
        }
        return tentativa.falhas >= MAX_TENTATIVAS;
    }

    public void registrarFalha(String chave) {
        tentativas.compute(chave, (k, atual) -> {
            long agora = System.currentTimeMillis();
            if (atual == null || agora - atual.inicioJanela >= JANELA_MS) {
                return new Tentativa(agora, 1);
            }
            atual.falhas++;
            return atual;
        });
    }

    public void resetar(String chave) {
        tentativas.remove(chave);
    }

    private boolean expirada(Tentativa tentativa) {
        return System.currentTimeMillis() - tentativa.inicioJanela >= JANELA_MS;
    }

    private void limparExpirados() {
        long agora = System.currentTimeMillis();
        Iterator<Map.Entry<String, Tentativa>> it = tentativas.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Tentativa> entrada = it.next();
            if (agora - entrada.getValue().inicioJanela >= JANELA_MS) {
                it.remove();
            }
        }
    }

    private static final class Tentativa {
        private final long inicioJanela;
        private int falhas;

        private Tentativa(long inicioJanela, int falhas) {
            this.inicioJanela = inicioJanela;
            this.falhas = falhas;
        }
    }
}
