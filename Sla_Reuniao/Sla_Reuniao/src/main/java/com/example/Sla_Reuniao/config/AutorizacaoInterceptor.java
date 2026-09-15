package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    private static final Set<String> PERFIS_CANCELAR_LIGACAO = Set.of("COLABORADOR", "ADMIN");
    private static final Set<String> PERFIS_CANCELAR_AGENDAMENTO = Set.of("COLABORADOR", "RECEPCAO", "ADMIN");

    private final UsuarioService usuarioService;

    public AutorizacaoInterceptor(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String rota = normalizarRota(request);
        String metodo = request.getMethod();

        if (ehPublica(rota)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UsuarioSessao usuarioLogado = extrairUsuario(session);

        if (usuarioLogado == null) {
            if (rota.startsWith("/api/")) {
                responderJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado");
            } else {
                response.sendRedirect("/");
            }
            return false;
        }

        if (!usuarioService.isAtivo(usuarioLogado.getId())) {
            session.invalidate();
            response.sendRedirect("/");
            return false;
        }

        String perfil = usuarioLogado.getPerfil();

        if (ehRotaAutenticadaCompartilhada(rota, metodo)) {
            return true;
        }

        if (rota.startsWith("/cancelar-ligacao")) {
            if (!"POST".equalsIgnoreCase(metodo) || !PERFIS_CANCELAR_LIGACAO.contains(perfil)) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        if (rota.startsWith("/cancelar-agendamento")) {
            if (!"POST".equalsIgnoreCase(metodo) || !PERFIS_CANCELAR_AGENDAMENTO.contains(perfil)) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        if (rota.startsWith("/admin")
                || rota.startsWith("/cadastrar-usuario")
                || rota.startsWith("/deletar-usuario")
                || rota.startsWith("/redefinir-senha")
                || rota.startsWith("/cadastrar-sala")
                || rota.startsWith("/cadastrar-contato")
                || ehMutacaoAdmin(rota, metodo)) {
            if (!"ADMIN".equals(perfil)) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        if (rota.startsWith("/recepcao") || rota.startsWith("/concluir-ligacao")) {
            if (!"RECEPCAO".equals(perfil)) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        if (rota.startsWith("/usuario")) {
            if (!"COLABORADOR".equals(perfil)) {
                response.sendRedirect(rotaPorPerfil(perfil));
                return false;
            }
            return true;
        }

        if (rota.startsWith("/api/")) {
            responderJson(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
            return false;
        }

        response.sendRedirect(rotaPorPerfil(perfil));
        return false;
    }

    // GET / precisa passar sem login: anônimo vê a tela de entrada. Quem já entrou é redirecionado no WebController.
    private boolean ehPublica(String rota) {
        return rota.equals("/")
                || rota.equals("/fazer-login")
                || rota.equals("/error")
                || rota.equals("/favicon.ico")
                || rota.startsWith("/css")
                || rota.startsWith("/js")
                || rota.startsWith("/images")
                || rota.startsWith("/webjars");
    }

    private boolean ehRotaAutenticadaCompartilhada(String rota, String metodo) {
        if (rota.equals("/sair")) {
            return "POST".equalsIgnoreCase(metodo);
        }
        if (rota.equals("/alterar-senha") || rota.startsWith("/alterar-senha/")) {
            return true;
        }
        return rota.startsWith("/kanban")
                || rota.startsWith("/criar-tarefa")
                || rota.startsWith("/editar-tarefa")
                || rota.startsWith("/excluir-tarefa")
                || rota.startsWith("/mover-tarefa")
                || rota.startsWith("/adicionar-comentario")
                || rota.startsWith("/enviar-historico-email")
                || rota.startsWith("/fazer-pedido-ligacao")
                || rota.startsWith("/fazer-agendamento");
    }

    private boolean ehMutacaoAdmin(String rota, String metodo) {
        if (!"POST".equalsIgnoreCase(metodo)) {
            return false;
        }
        return rota.startsWith("/editar-contato")
                || rota.startsWith("/arquivar-contato")
                || rota.startsWith("/editar-sala")
                || rota.startsWith("/arquivar-sala");
    }

    private String normalizarRota(HttpServletRequest request) {
        String rota = request.getServletPath();
        if (rota == null || rota.isBlank()) {
            String pathInfo = request.getPathInfo();
            rota = pathInfo != null && !pathInfo.isBlank() ? pathInfo : request.getRequestURI();
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && rota.startsWith(ctx)) {
            rota = rota.substring(ctx.length());
        }
        int separador = rota.indexOf(';');
        if (separador >= 0) {
            rota = rota.substring(0, separador);
        }
        if (rota.isBlank()) {
            rota = "/";
        } else if (rota.length() > 1 && rota.endsWith("/")) {
            rota = rota.substring(0, rota.length() - 1);
        }
        return rota;
    }

    private UsuarioSessao extrairUsuario(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object atributo = session.getAttribute("usuarioLogado");
        if (atributo instanceof UsuarioSessao sessao) {
            return sessao;
        }
        return null;
    }

    private void responderJson(HttpServletResponse response, int status, String mensagem) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }

    private String rotaPorPerfil(String perfil) {
        return switch (perfil) {
            case "ADMIN" -> "/admin";
            case "RECEPCAO" -> "/recepcao";
            default -> "/usuario";
        };
    }
}
