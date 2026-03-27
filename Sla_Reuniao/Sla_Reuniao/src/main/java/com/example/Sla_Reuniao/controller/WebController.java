package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.model.*;
import com.example.Sla_Reuniao.repository.*;
import com.example.Sla_Reuniao.service.AgendamentoService;
import com.example.Sla_Reuniao.service.TarefaService;
import com.example.Sla_Reuniao.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class WebController {

    private final SalaRepository salaRepository;
    private final PedidoLigacaoRepository pedidoLigacaoRepository;
    private final ContatoAgendaRepository contatoAgendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoService agendamentoService;
    private final PasswordEncoder passwordEncoder;
    private final TarefaRepository tarefaRepository;
    private final TarefaService tarefaService;
    private final ComentarioRepository comentarioRepository;
    private final EmailService emailService;

    public WebController(SalaRepository salaRepository,
                         PedidoLigacaoRepository pedidoLigacaoRepository,
                         ContatoAgendaRepository contatoAgendaRepository,
                         UsuarioRepository usuarioRepository,
                         AgendamentoRepository agendamentoRepository,
                         AgendamentoService agendamentoService,
                         PasswordEncoder passwordEncoder,
                         TarefaRepository tarefaRepository,
                         TarefaService tarefaService,
                         ComentarioRepository comentarioRepository,
                         EmailService emailService) {
        this.salaRepository = salaRepository;
        this.pedidoLigacaoRepository = pedidoLigacaoRepository;
        this.contatoAgendaRepository = contatoAgendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.agendamentoService = agendamentoService;
        this.passwordEncoder = passwordEncoder;
        this.tarefaRepository = tarefaRepository;
        this.tarefaService = tarefaService;
        this.comentarioRepository = comentarioRepository;
        this.emailService = emailService;
    }

    private Usuario getUsuarioLogado(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogado");
    }

    private String rotaPorPerfil(String perfil) {
        return switch (perfil) {
            case "ADMIN"    -> "/admin";
            case "RECEPCAO" -> "/recepcao";
            default         -> "/usuario";
        };
    }


    @GetMapping("/")
    public String telaLogin() {
        return "login";
    }

    @PostMapping("/fazer-login")
    public String fazerLogin(@RequestParam String email,
                             @RequestParam String senha,
                             HttpSession session,
                             Model model) {
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario != null && passwordEncoder.matches(senha, usuario.getSenha())) {
            session.setAttribute("usuarioLogado", usuario);
            return "redirect:" + rotaPorPerfil(usuario.getPerfil());
        }

        model.addAttribute("erro", "E-mail ou senha incorretos.");
        return "login";
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    @GetMapping("/kanban")
    public String mostrarKanban(HttpSession session, Model model) {
        Usuario usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("todosUsuarios", usuarioRepository.findAll());

        boolean isAdmin = "ADMIN".equals(usuarioLogado.getPerfil());

        if (isAdmin) {
            model.addAttribute("tarefasAFazer",     tarefaRepository.findByStatus("A_FAZER"));
            model.addAttribute("tarefasFazendo",    tarefaRepository.findByStatus("FAZENDO"));
            model.addAttribute("tarefasConcluidas", tarefaRepository.findByStatus("CONCLUIDO"));
            model.addAttribute("todasTarefas",      tarefaRepository.findAll());
        } else {
            String nome = usuarioLogado.getNome();
            model.addAttribute("tarefasAFazer",     tarefaRepository.findByStatusAndUsuarioEnvolvido("A_FAZER",   usuarioLogado, nome));
            model.addAttribute("tarefasFazendo",    tarefaRepository.findByStatusAndUsuarioEnvolvido("FAZENDO",   usuarioLogado, nome));
            model.addAttribute("tarefasConcluidas", tarefaRepository.findByStatusAndUsuarioEnvolvido("CONCLUIDO", usuarioLogado, nome));
            model.addAttribute("todasTarefas",      tarefaRepository.findAllByUsuarioEnvolvido(usuarioLogado, nome));
        }

        return "kanban";
    }

    @PostMapping("/criar-tarefa")
    public String criarTarefa(@ModelAttribute Tarefa novaTarefa,
                              @RequestParam(required = false) List<Long> responsaveisIds) {
        if (responsaveisIds != null && !responsaveisIds.isEmpty()) {
            novaTarefa.setResponsaveis(usuarioRepository.findAllById(responsaveisIds));
        }
        tarefaRepository.save(novaTarefa);
        return "redirect:/kanban";
    }

    @PostMapping("/mover-tarefa/{id}")
    public String moverTarefa(@PathVariable Long id, @RequestParam String novoStatus) {
        tarefaService.moverTarefa(id, novoStatus);
        return "redirect:/kanban";
    }

    @PostMapping("/adicionar-comentario/{tarefaId}")
    public String adicionarComentario(@PathVariable Long tarefaId,
                                      @RequestParam String texto,
                                      HttpSession session) {
        Usuario usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        tarefaRepository.findById(tarefaId).ifPresent(tarefa -> {
            Comentario comentario = new Comentario();
            comentario.setTexto(texto);
            comentario.setAutor(usuarioLogado.getNome());
            comentario.setTarefa(tarefa);
            comentarioRepository.save(comentario);
        });

        return "redirect:/kanban";
    }

    @PostMapping("/enviar-historico-email/{tarefaId}")
    public String enviarHistoricoEmail(@PathVariable Long tarefaId) {
        tarefaRepository.findById(tarefaId).ifPresent(tarefa -> {
            String corpo = montarCorpoEmailHistorico(tarefa);
            String assunto = "Cópia do Chamado #" + tarefa.getId();

            tarefa.getResponsaveis().forEach(r -> enviarEmailSilencioso(r.getEmail(), assunto, corpo));

            usuarioRepository.findAll().stream()
                    .filter(u -> u.getNome().equals(tarefa.getSolicitante()))
                    .findFirst()
                    .ifPresent(s -> enviarEmailSilencioso(s.getEmail(), assunto, corpo));
        });

        return "redirect:/kanban";
    }


    @GetMapping("/recepcao")
    public String mostrarFilaRecepcao(HttpSession session, Model model) {
        if (getUsuarioLogado(session) == null) return "redirect:/";

        List<PedidoLigacao> pendentes = pedidoLigacaoRepository.findByStatus("PENDENTE");

        model.addAttribute("listaDeLigacoes",   pendentes);
        model.addAttribute("totalPendentes",    pendentes.size());
        model.addAttribute("listaAgendamentos",
                agendamentoRepository.findByDataHoraFimAfterOrderByDataHoraInicioAsc(LocalDateTime.now()));

        return "fila-chamados";
    }

    @PostMapping("/concluir-ligacao/{id}")
    public String concluirLigacao(@PathVariable Long id) {
        pedidoLigacaoRepository.findById(id).ifPresent(pedido -> {
            pedido.setStatus("CONCLUIDO");
            pedidoLigacaoRepository.save(pedido);
        });
        return "redirect:/recepcao";
    }


    @GetMapping("/usuario")
    public String mostrarPortalUsuario(HttpSession session, Model model) {
        Usuario usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        List<PedidoLigacao> minhasLigacoes = pedidoLigacaoRepository
                .findByNomeColaboradorOrderByDataSolicitacaoDesc(usuarioLogado.getNome());
        minhasLigacoes.removeIf(l -> l.getDestinatario() == null || l.getDestinatario().isBlank());

        long pendentes = pedidoLigacaoRepository
                .countByNomeColaboradorAndStatus(usuarioLogado.getNome(), "PENDENTE");

        model.addAttribute("usuarioLogado",           usuarioLogado);
        model.addAttribute("listaDeSalas",            salaRepository.findAll());
        model.addAttribute("listaAgenda",             contatoAgendaRepository.findAll());
        model.addAttribute("listaAgendamentos",
                agendamentoRepository.findByDataHoraFimAfterOrderByDataHoraInicioAsc(LocalDateTime.now()));
        model.addAttribute("minhasLigacoes",          minhasLigacoes);
        model.addAttribute("minhasLigacoesPendentes", pendentes);

        return "portal-usuario";
    }

    @PostMapping("/fazer-pedido-ligacao")
    public String salvarPedidoLigacao(@ModelAttribute PedidoLigacao novoPedido,
                                      HttpSession session) {
        pedidoLigacaoRepository.save(novoPedido);
        Usuario logado = getUsuarioLogado(session);
        return "redirect:" + (logado != null ? rotaPorPerfil(logado.getPerfil()) : "/");
    }


    @PostMapping("/fazer-agendamento")
    public String fazerAgendamento(@ModelAttribute Agendamento novoAgendamento,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Usuario logado = getUsuarioLogado(session);

        String voltaPara = logado != null ? rotaPorPerfil(logado.getPerfil()) : "/";

        try {
            agendamentoService.validarESalvar(novoAgendamento);
            redirectAttributes.addFlashAttribute("sucessoAgendamento", " $ Sala reservada com sucesso!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAgendamento", " X " + e.getMessage());
        }

        return "redirect:" + voltaPara;
    }


    @GetMapping("/admin")
    public String mostrarPortalAdmin(HttpSession session, Model model) {
        Usuario usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("qtdAFazer",     tarefaRepository.findByStatus("A_FAZER").size());
        model.addAttribute("qtdFazendo",    tarefaRepository.findByStatus("FAZENDO").size());
        model.addAttribute("qtdConcluido",  tarefaRepository.findByStatus("CONCLUIDO").size());
        model.addAttribute("qtdUsuarios",   usuarioRepository.count());
        model.addAttribute("listaDeSalas",  salaRepository.findAll());
        model.addAttribute("listaAgenda",   contatoAgendaRepository.findAll());
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());

        return "portal-admin";
    }

    @PostMapping("/cadastrar-sala")
    public String cadastrarNovaSala(@ModelAttribute Sala novaSala) {
        novaSala.setDisponivel(true);
        salaRepository.save(novaSala);
        return "redirect:/admin";
    }

    @PostMapping("/cadastrar-contato")
    public String cadastrarNovoContato(@ModelAttribute ContatoAgenda novoContato) {
        contatoAgendaRepository.save(novoContato);
        return "redirect:/admin";
    }

    @PostMapping("/cadastrar-usuario")
    public String cadastrarNovoUsuario(@ModelAttribute Usuario novoUsuario) {
        novoUsuario.setSenha(passwordEncoder.encode(novoUsuario.getSenha()));
        usuarioRepository.save(novoUsuario);
        return "redirect:/admin";
    }

    @PostMapping("/deletar-usuario/{id}")
    public String deletarUsuario(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado != null && !usuarioLogado.getId().equals(id)) {
            usuarioRepository.deleteById(id);
        }
        return "redirect:/admin";
    }

    @PostMapping("/redefinir-senha/{id}")
    public String redefinirSenha(@PathVariable Long id, @RequestParam String novaSenha) {
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setSenha(passwordEncoder.encode(novaSenha));
            usuarioRepository.save(usuario);
        });
        return "redirect:/admin";
    }

    @GetMapping("/admin/exportar-relatorio")
    public void exportarRelatorioExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Relatorio_SeOrganize.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheetTarefas = workbook.createSheet("Resumo de Tarefas");
            criarCabecalho(sheetTarefas, "ID", "Título", "Solicitante", "Status");
            int row = 1;
            for (Tarefa t : tarefaRepository.findAll()) {
                Row r = sheetTarefas.createRow(row++);
                r.createCell(0).setCellValue(t.getId());
                r.createCell(1).setCellValue(t.getTitulo());
                r.createCell(2).setCellValue(t.getSolicitante());
                r.createCell(3).setCellValue(t.getStatus());
            }

            Sheet sheetUsuarios = workbook.createSheet("Equipe Ativa");
            criarCabecalho(sheetUsuarios, "Nome", "E-mail", "Perfil");
            int rowU = 1;
            for (Usuario u : usuarioRepository.findAll()) {
                Row r = sheetUsuarios.createRow(rowU++);
                r.createCell(0).setCellValue(u.getNome());
                r.createCell(1).setCellValue(u.getEmail());
                r.createCell(2).setCellValue(u.getPerfil());
            }

            workbook.write(response.getOutputStream());
        }
    }


    private String montarCorpoEmailHistorico(Tarefa tarefa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá!\n\nAqui está a cópia oficial do histórico do chamado.\n\n");
        sb.append(" TAREFA #").append(tarefa.getId()).append(" — ").append(tarefa.getTitulo()).append("\n");
        sb.append("Descrição: ").append(tarefa.getDescricao()).append("\n");
        sb.append("Status Atual: ").append(tarefa.getStatus()).append("\n\n");
        sb.append("💬 HISTÓRICO DE CONVERSA:\n");

        if (tarefa.getComentarios().isEmpty()) {
            sb.append("Nenhum comentário registrado.\n");
        } else {
            for (Comentario c : tarefa.getComentarios()) {
                sb.append(" > ").append(c.getAutor()).append(": ").append(c.getTexto()).append("\n");
            }
        }
        sb.append("\n\n--\nEnviado via Sistema SeOrganize");
        return sb.toString();
    }

    private void enviarEmailSilencioso(String destinatario, String assunto, String corpo) {
        try {
            emailService.enviarEmail(destinatario, assunto, corpo);
        } catch (Exception e) {
            System.err.println("Falha ao enviar e-mail para " + destinatario + ": " + e.getMessage());
        }
    }

    private void criarCabecalho(Sheet sheet, String... colunas) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            header.createCell(i).setCellValue(colunas[i]);
        }
    }
}