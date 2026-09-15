package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    private static final Logger log = LoggerFactory.getLogger(PerfilController.class);

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/alterar-senha")
    public String mostrarAlterarSenha(HttpSession session, Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogado", usuarioLogado);
        return "alterar-senha";
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@RequestParam("senhaAtual") String senhaAtual,
                               @RequestParam("senhaNova") String senhaNova,
                               @RequestParam(value = "senhaConfirmacao", required = false) String senhaConfirmacao,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        if (senhaAtual == null || senhaAtual.isBlank()) {
            redirectAttributes.addFlashAttribute("senhaErro", "Informe a senha atual.");
            return "redirect:/alterar-senha";
        }

        if (senhaNova == null || senhaConfirmacao == null || !senhaNova.equals(senhaConfirmacao)) {
            redirectAttributes.addFlashAttribute("senhaErro", "A nova senha e a confirmação não coincidem.");
            return "redirect:/alterar-senha";
        }

        try {
            usuarioService.alterarSenha(usuarioLogado.getId(), usuarioLogado.getEmail(), senhaAtual, senhaNova);
            redirectAttributes.addFlashAttribute("erro", "Senha alterada. Entre novamente com a nova senha.");
            session.invalidate();
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("senhaErro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível alterar a senha.");
        } catch (Exception e) {
            log.error("Falha ao alterar senha do usuário {}", usuarioLogado.getEmail(), e);
            redirectAttributes.addFlashAttribute("senhaErro", "Não foi possível alterar a senha.");
        }

        return "redirect:/alterar-senha";
    }

    private UsuarioSessao getUsuarioLogado(HttpSession session) {
        Object atributo = session.getAttribute("usuarioLogado");
        return atributo instanceof UsuarioSessao sessao ? sessao : null;
    }
}
