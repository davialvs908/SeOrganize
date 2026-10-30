package com.example.Sla_Reuniao.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Proteções contra abuso (rate limit)")
class RateLimiterTest {

    @Nested
    @DisplayName("LoginRateLimiter")
    class Login {

        private final LoginRateLimiter limiter = new LoginRateLimiter();

        @Test
        @DisplayName("não bloqueia quem ainda não errou a senha")
        void naoBloqueiaSemFalhas() {
            assertThat(limiter.estaBloqueado("ana@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("bloqueia depois de 5 tentativas erradas")
        void bloqueiaAposCincoFalhas() {
            for (int i = 0; i < 4; i++) {
                limiter.registrarFalha("ana@empresa.com");
            }
            assertThat(limiter.estaBloqueado("ana@empresa.com")).isFalse();

            limiter.registrarFalha("ana@empresa.com");
            assertThat(limiter.estaBloqueado("ana@empresa.com")).isTrue();
        }

        @Test
        @DisplayName("libera o acesso depois de um login bem-sucedido")
        void resetarLibera() {
            for (int i = 0; i < 5; i++) {
                limiter.registrarFalha("ana@empresa.com");
            }
            limiter.resetar("ana@empresa.com");

            assertThat(limiter.estaBloqueado("ana@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("as tentativas de um usuário não afetam outro")
        void chavesIndependentes() {
            for (int i = 0; i < 5; i++) {
                limiter.registrarFalha("ana@empresa.com");
            }
            assertThat(limiter.estaBloqueado("bruno@empresa.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("MutationRateLimiter")
    class Mutacoes {

        private final MutationRateLimiter limiter = new MutationRateLimiter();

        @Test
        @DisplayName("permite até 30 tarefas por janela e bloqueia a 31ª")
        void limitaCriacaoDeTarefas() {
            for (int i = 0; i < 30; i++) {
                assertThat(limiter.allowCriarTarefa("sessao-1")).isTrue();
            }
            assertThat(limiter.allowCriarTarefa("sessao-1")).isFalse();
        }

        @Test
        @DisplayName("os limites de tarefas e de ligações são separados")
        void limitesSeparados() {
            for (int i = 0; i < 30; i++) {
                limiter.allowCriarTarefa("sessao-1");
            }
            assertThat(limiter.allowCriarLigacao("sessao-1")).isTrue();
        }
    }
}
