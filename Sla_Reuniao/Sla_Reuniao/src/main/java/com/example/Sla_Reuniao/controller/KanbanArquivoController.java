package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.repository.TarefaRepository;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class KanbanArquivoController {

    private static final int TAMANHO_PAGINA = 20;

    private final TarefaRepository tarefaRepository;

    public KanbanArquivoController(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    @GetMapping("/kanban/arquivo")
    public String mostrarArquivo(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) Integer pagina,
                                 HttpSession session,
                                 Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        List<Tarefa> concluidas = usuarioLogado.isAdmin()
                ? tarefaRepository.findByStatus("CONCLUIDO")
                : tarefaRepository.findByStatusAndUsuarioEnvolvido(
                        "CONCLUIDO", usuarioLogado.getId(), usuarioLogado.getNome());

        String busca = q == null ? "" : q.trim();
        if (!busca.isEmpty()) {
            String termo = busca.toLowerCase(Locale.ROOT);
            concluidas = concluidas.stream()
                    .filter(tarefa -> contém(tarefa.getTitulo(), termo) || contém(tarefa.getDescricao(), termo))
                    .toList();
        }

        List<Tarefa> ordenadas = concluidas.stream()
                .sorted(Comparator.comparing(Tarefa::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = ordenadas.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) TAMANHO_PAGINA));
        int paginaAtual = pagina == null || pagina < 1 ? 1 : Math.min(pagina, totalPaginas);
        int inicio = (paginaAtual - 1) * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, total);
        List<Tarefa> paginaTarefas = inicio >= total ? List.of() : ordenadas.subList(inicio, fim);

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("q", busca.isEmpty() ? null : busca);
        model.addAttribute("pagina", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("tarefas", paginaTarefas);
        return "kanban-arquivo";
    }

    private static boolean contém(String texto, String termo) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(termo);
    }

    private UsuarioSessao getUsuarioLogado(HttpSession session) {
        Object atributo = session.getAttribute("usuarioLogado");
        return atributo instanceof UsuarioSessao sessao ? sessao : null;
    }
}
