package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.ContatoAgendaService;
import com.example.Sla_Reuniao.service.SalaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCatalogoController {

    private final ContatoAgendaService contatoAgendaService;
    private final SalaService salaService;

    public AdminCatalogoController(ContatoAgendaService contatoAgendaService, SalaService salaService) {
        this.contatoAgendaService = contatoAgendaService;
        this.salaService = salaService;
    }

    @PostMapping("/editar-contato")
    public String editarContato(@RequestParam Long id,
                                @RequestParam String nome,
                                @RequestParam String telefone,
                                @RequestParam(required = false) String departamento,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            return "redirect:/";
        }
        try {
            contatoAgendaService.atualizar(id, nome, telefone, departamento);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Contato atualizado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/arquivar-contato/{id}")
    public String arquivarContato(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            return "redirect:/";
        }
        try {
            contatoAgendaService.arquivar(id);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Contato arquivado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/editar-sala")
    public String editarSala(@RequestParam Long id,
                             @RequestParam String nome,
                             @RequestParam Integer capacidade,
                             @RequestParam(required = false) String recursos,
                             @RequestParam String localizacao,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            return "redirect:/";
        }
        try {
            salaService.atualizar(id, nome, capacidade, recursos, localizacao);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Sala atualizada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/arquivar-sala/{id}")
    public String arquivarSala(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            return "redirect:/";
        }
        try {
            salaService.arquivar(id);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Sala arquivada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    private boolean ehAdmin(HttpSession session) {
        UsuarioSessao usuario = getUsuarioLogado(session);
        return usuario != null && usuario.isAdmin();
    }

    private UsuarioSessao getUsuarioLogado(HttpSession session) {
        Object atributo = session.getAttribute("usuarioLogado");
        return atributo instanceof UsuarioSessao sessao ? sessao : null;
    }
}
