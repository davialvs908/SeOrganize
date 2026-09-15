package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.PedidoLigacaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LigacaoWebController {

    private final PedidoLigacaoService pedidoLigacaoService;

    public LigacaoWebController(PedidoLigacaoService pedidoLigacaoService) {
        this.pedidoLigacaoService = pedidoLigacaoService;
    }

    @PostMapping("/cancelar-ligacao/{id}")
    public String cancelarLigacao(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        try {
            pedidoLigacaoService.cancelarSePendente(id, usuarioLogado);
            redirectAttributes.addFlashAttribute("sucessoAgendamento", "Pedido de ligação cancelado.");
        } catch (AcessoNegadoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAgendamento", e.getMessage());
        }

        return "redirect:" + rotaPorPerfil(usuarioLogado.getPerfil());
    }

    private UsuarioSessao getUsuarioLogado(HttpSession session) {
        Object atributo = session.getAttribute("usuarioLogado");
        return atributo instanceof UsuarioSessao sessao ? sessao : null;
    }

    private String rotaPorPerfil(String perfil) {
        return switch (perfil) {
            case "ADMIN" -> "/admin";
            case "RECEPCAO" -> "/recepcao";
            default -> "/usuario";
        };
    }
}
