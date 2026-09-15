package com.example.Sla_Reuniao.security;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MutationRateLimiter {

    private static final int MAX_CRIACOES = 30;
    private static final long JANELA_MS = TimeUnit.MINUTES.toMillis(10);

    private final ConcurrentHashMap<String, Contagem> tarefas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Contagem> ligacoes = new ConcurrentHashMap<>();

    public boolean allowCriarTarefa(String chave) {
        return permitir(tarefas, chave);
    }

    public boolean allowCriarLigacao(String chave) {
        return permitir(ligacoes, chave);
    }

    private boolean permitir(ConcurrentHashMap<String, Contagem> mapa, String chave) {
        limparExpirados(mapa);
        AtomicBoolean permitido = new AtomicBoolean(false);
        mapa.compute(chave, (k, atual) -> {
            long agora = System.currentTimeMillis();
            if (atual == null || agora - atual.inicioJanela >= JANELA_MS) {
                permitido.set(true);
                return new Contagem(agora, 1);
            }
            if (atual.quantidade >= MAX_CRIACOES) {
                return atual;
            }
            atual.quantidade++;
            permitido.set(true);
            return atual;
        });
        return permitido.get();
    }

    private void limparExpirados(ConcurrentHashMap<String, Contagem> mapa) {
        long agora = System.currentTimeMillis();
        Iterator<Map.Entry<String, Contagem>> it = mapa.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Contagem> entrada = it.next();
            if (agora - entrada.getValue().inicioJanela >= JANELA_MS) {
                it.remove();
            }
        }
    }

    private static final class Contagem {
        private final long inicioJanela;
        private int quantidade;

        private Contagem(long inicioJanela, int quantidade) {
            this.inicioJanela = inicioJanela;
            this.quantidade = quantidade;
        }
    }
}
