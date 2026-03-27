package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String rota = request.getRequestURI();


        if (rota.equals("/") || rota.equals("/fazer-login") || rota.equals("/sair")
                || rota.startsWith("/css") || rota.startsWith("/js")) {
            return true;
        }


        HttpSession session = request.getSession();
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            response.sendRedirect("/");
            return false;
        }

        String perfil = usuarioLogado.getPerfil();


        if (rota.startsWith("/kanban")
                || rota.startsWith("/criar-tarefa")
                || rota.startsWith("/mover-tarefa")
                || rota.startsWith("/adicionar-comentario")
                || rota.startsWith("/enviar-historico-email")
                || rota.startsWith("/fazer-pedido-ligacao")
                || rota.startsWith("/fazer-agendamento")) {
            return true;
        }


        if (rota.startsWith("/admin")
                || rota.startsWith("/cadastrar-usuario")
                || rota.startsWith("/deletar-usuario")
                || rota.startsWith("/redefinir-senha")
                || rota.startsWith("/cadastrar-sala")
                || rota.startsWith("/cadastrar-contato")) {
            if (!perfil.equals("ADMIN")) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }


        if (rota.startsWith("/recepcao")
                || rota.startsWith("/concluir-ligacao")) {
            if (!perfil.equals("RECEPCAO")) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }


        if (rota.startsWith("/usuario")) {
            if (!perfil.equals("COLABORADOR")) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        return true;
    }

    private String rotaPorPerfil(String perfil) {
        return switch (perfil) {
            case "ADMIN"    -> "/admin";
            case "RECEPCAO" -> "/recepcao";
            default         -> "/usuario";
        };
    }
}