package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.dto.ContatoAgendaForm;
import com.example.Sla_Reuniao.dto.PedidoLigacaoForm;
import com.example.Sla_Reuniao.dto.SalaForm;
import com.example.Sla_Reuniao.dto.UsuarioForm;
import com.example.Sla_Reuniao.model.*;
import com.example.Sla_Reuniao.repository.*;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.LoginRateLimiter;
import com.example.Sla_Reuniao.security.MutationRateLimiter;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import com.example.Sla_Reuniao.service.AgendamentoService;
import com.example.Sla_Reuniao.service.ContatoAgendaService;
import com.example.Sla_Reuniao.service.EmailService;
import com.example.Sla_Reuniao.service.ExcelExportService;
import com.example.Sla_Reuniao.service.PedidoLigacaoService;
import com.example.Sla_Reuniao.service.SalaService;
import com.example.Sla_Reuniao.service.TarefaService;
import com.example.Sla_Reuniao.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    private final PedidoLigacaoRepository pedidoLigacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoService agendamentoService;
    private final PedidoLigacaoService pedidoLigacaoService;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final TarefaRepository tarefaRepository;
    private final TarefaService tarefaService;
    private final ExcelExportService excelExportService;
    private final EmailService emailService;
    private final LoginRateLimiter loginRateLimiter;
    private final MutationRateLimiter mutationRateLimiter;
    private final ContatoAgendaService contatoAgendaService;
    private final SalaService salaService;

    public WebController(PedidoLigacaoRepository pedidoLigacaoRepository,
                         UsuarioRepository usuarioRepository,
                         AgendamentoService agendamentoService,
                         PedidoLigacaoService pedidoLigacaoService,
                         UsuarioService usuarioService,
                         PasswordEncoder passwordEncoder,
                         TarefaRepository tarefaRepository,
                         TarefaService tarefaService,
                         ExcelExportService excelExportService,
                         EmailService emailService,
                         LoginRateLimiter loginRateLimiter,
                         MutationRateLimiter mutationRateLimiter,
                         ContatoAgendaService contatoAgendaService,
                         SalaService salaService) {
        this.pedidoLigacaoRepository = pedidoLigacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoService = agendamentoService;
        this.pedidoLigacaoService = pedidoLigacaoService;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.tarefaRepository = tarefaRepository;
        this.tarefaService = tarefaService;
        this.excelExportService = excelExportService;
        this.emailService = emailService;
        this.loginRateLimiter = loginRateLimiter;
        this.mutationRateLimiter = mutationRateLimiter;
        this.contatoAgendaService = contatoAgendaService;
        this.salaService = salaService;
    }

    private UsuarioSessao getUsuarioLogado(HttpSession session) {
        Object atributo = session.getAttribute("usuarioLogado");
        return atributo instanceof UsuarioSessao sessao ? sessao : null;
    }

    private String saudacaoAgora() {
        int hora = LocalDateTime.now().getHour();
        if (hora < 12) return "Bom dia";
        if (hora < 18) return "Boa tarde";
        return "Boa noite";
    }

    private String rotaPorPerfil(String perfil) {
        return switch (perfil) {
            case "ADMIN" -> "/admin";
            case "RECEPCAO" -> "/recepcao";
            default -> "/usuario";
        };
    }

    private String chaveRateLimit(HttpServletRequest request, String email) {
        String ip = request.getRemoteAddr();
        String emailNorm = email == null ? "" : email.trim().toLowerCase();
        return ip + ":" + emailNorm;
    }

    private void adicionarResumoTarefas(Model model, UsuarioSessao usuario) {
        if (usuario.isAdmin()) {
            model.addAttribute("qtdAFazer", tarefaRepository.countByStatus("A_FAZER"));
            model.addAttribute("qtdFazendo", tarefaRepository.countByStatus("FAZENDO"));
            model.addAttribute("qtdConcluido", tarefaRepository.countByStatus("CONCLUIDO"));
        } else {
            Long id = usuario.getId();
            String nome = usuario.getNome();
            model.addAttribute("qtdAFazer", tarefaRepository.findByStatusAndUsuarioEnvolvido("A_FAZER", id, nome).size());
            model.addAttribute("qtdFazendo", tarefaRepository.findByStatusAndUsuarioEnvolvido("FAZENDO", id, nome).size());
            model.addAttribute("qtdConcluido", tarefaRepository.findByStatusAndUsuarioEnvolvido("CONCLUIDO", id, nome).size());
        }
    }


    @GetMapping("/")
    public String telaLogin(HttpSession session) {
        // o logo da topbar aponta pra cá; se já tem sessão, manda pro portal do perfil em vez de reabrir o login
        UsuarioSessao logado = getUsuarioLogado(session);
        if (logado != null) {
            return "redirect:" + rotaPorPerfil(logado.getPerfil());
        }
        return "login";
    }

    @PostMapping("/fazer-login")
    public String fazerLogin(@RequestParam String email,
                             @RequestParam String senha,
                             HttpServletRequest request,
                             HttpSession session,
                             Model model) {
        String chave = chaveRateLimit(request, email);
        if (loginRateLimiter.estaBloqueado(chave)) {
            log.warn("Login bloqueado por excesso de tentativas: {}", email);
            model.addAttribute("erro", "Muitas tentativas. Aguarde 15 minutos e tente novamente.");
            return "login";
        }

        String emailNorm = email == null ? "" : email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(emailNorm);

        if (usuario != null && passwordEncoder.matches(senha, usuario.getSenha())) {
            if (!usuarioService.isAtivo(usuario.getEmail())) {
                model.addAttribute("erro", "Usuário inativo. Contate o administrador.");
                return "login";
            }
            loginRateLimiter.resetar(chave);
            session.invalidate();
            HttpSession novaSessao = request.getSession(true);
            novaSessao.setAttribute("usuarioLogado", UsuarioSessao.from(usuario));
            log.info("Login bem-sucedido: {} ({})", usuario.getEmail(), usuario.getPerfil());
            return "redirect:" + rotaPorPerfil(usuario.getPerfil());
        }

        loginRateLimiter.registrarFalha(chave);
        log.warn("Falha de login para o e-mail {}", email);
        model.addAttribute("erro", "E-mail ou senha incorretos.");
        return "login";
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    @GetMapping("/kanban")
    public String mostrarKanban(@RequestParam(required = false) Long chat,
                                HttpSession session,
                                Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("saudacao", saudacaoAgora());
        model.addAttribute("todosUsuarios", usuarioRepository.findAll());
        model.addAttribute("chatAberto", chat);

        boolean isAdmin = usuarioLogado.isAdmin();

        if (isAdmin) {
            model.addAttribute("tarefasAFazer", tarefaRepository.findByStatus("A_FAZER"));
            model.addAttribute("tarefasFazendo", tarefaRepository.findByStatus("FAZENDO"));
            model.addAttribute("tarefasConcluidas", tarefaRepository.findByStatus("CONCLUIDO"));
            model.addAttribute("todasTarefas", tarefaRepository.findAll());
        } else {
            Long id = usuarioLogado.getId();
            String nome = usuarioLogado.getNome();
            model.addAttribute("tarefasAFazer", tarefaRepository.findByStatusAndUsuarioEnvolvido("A_FAZER", id, nome));
            model.addAttribute("tarefasFazendo", tarefaRepository.findByStatusAndUsuarioEnvolvido("FAZENDO", id, nome));
            model.addAttribute("tarefasConcluidas", tarefaRepository.findByStatusAndUsuarioEnvolvido("CONCLUIDO", id, nome));
            model.addAttribute("todasTarefas", tarefaRepository.findAllByUsuarioEnvolvido(id, nome));
        }

        return "kanban";
    }

    @PostMapping("/criar-tarefa")
    public String criarTarefa(@RequestParam String titulo,
                              @RequestParam String descricao,
                              @RequestParam(required = false) List<Long> responsaveisIds,
                              HttpServletRequest request,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        if (!mutationRateLimiter.allowCriarTarefa(chaveRateLimit(request, usuarioLogado.getEmail()))) {
            redirectAttributes.addFlashAttribute("erroKanban", "Muitas solicitações. Aguarde alguns minutos e tente novamente.");
            return "redirect:/kanban";
        }

        if (titulo == null || titulo.isBlank() || descricao == null || descricao.isBlank()) {
            redirectAttributes.addFlashAttribute("erroKanban", "Título e descrição são obrigatórios.");
            return "redirect:/kanban";
        }

        tarefaService.criar(titulo.trim(), descricao.trim(), responsaveisIds, usuarioLogado);
        redirectAttributes.addFlashAttribute("sucessoKanban", "Tarefa criada no quadro.");
        return "redirect:/kanban";
    }

    @PostMapping("/editar-tarefa/{id}")
    public String editarTarefa(@PathVariable Long id,
                               @RequestParam String titulo,
                               @RequestParam String descricao,
                               @RequestParam(required = false) List<Long> responsaveisIds,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            tarefaService.editar(id, titulo.trim(), descricao.trim(), responsaveisIds, usuarioLogado);
            redirectAttributes.addFlashAttribute("sucessoKanban", "Tarefa atualizada.");
        } catch (AcessoNegadoException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        }
        return "redirect:/kanban";
    }

    @PostMapping("/excluir-tarefa/{id}")
    public String excluirTarefa(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            tarefaService.excluir(id, usuarioLogado);
            redirectAttributes.addFlashAttribute("sucessoKanban", "Tarefa excluída.");
        } catch (AcessoNegadoException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        }
        return "redirect:/kanban";
    }

    @PostMapping("/mover-tarefa/{id}")
    public String moverTarefa(@PathVariable Long id,
                              @RequestParam String novoStatus,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            tarefaService.moverTarefa(id, novoStatus, usuarioLogado);
            redirectAttributes.addFlashAttribute("sucessoKanban", "Tarefa movida.");
        } catch (AcessoNegadoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        }
        return "redirect:/kanban";
    }

    @PostMapping(value = "/mover-tarefa/{id}", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moverTarefaAjax(@PathVariable Long id,
                                                               @RequestParam String novoStatus,
                                                               HttpSession session) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "Não autenticado"));
        }
        try {
            tarefaService.moverTarefa(id, novoStatus, usuarioLogado);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (AcessoNegadoException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/adicionar-comentario/{tarefaId}")
    public String adicionarComentario(@PathVariable Long tarefaId,
                                      @RequestParam String texto,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            tarefaService.adicionarComentario(tarefaId, texto, usuarioLogado);
            redirectAttributes.addFlashAttribute("sucessoKanban", "Comentário adicionado.");
        } catch (AcessoNegadoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
            return "redirect:/kanban";
        }

        return "redirect:/kanban?chat=" + tarefaId;
    }

    @PostMapping("/enviar-historico-email/{tarefaId}")
    public String enviarHistoricoEmail(@PathVariable Long tarefaId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            Tarefa tarefa = tarefaService.buscarOuFalhar(tarefaId);
            tarefaService.garantirAcesso(tarefa, usuarioLogado);

            String corpo = montarCorpoEmailHistorico(tarefa);
            String assunto = "Cópia do Chamado #" + tarefa.getId();
            Set<String> destinatarios = destinatariosHistorico(tarefa);

            if (destinatarios.isEmpty()) {
                redirectAttributes.addFlashAttribute("erroKanban", "Não há destinatário para enviar o histórico.");
                return "redirect:/kanban?chat=" + tarefaId;
            }

            boolean falhou = false;
            for (String email : destinatarios) {
                if (!emailService.enviarEmailSilencioso(email, assunto, corpo)) {
                    falhou = true;
                }
            }
            if (falhou) {
                redirectAttributes.addFlashAttribute("erroKanban", "Não foi possível enviar o histórico por e-mail.");
            } else {
                redirectAttributes.addFlashAttribute("sucessoKanban", "Histórico enviado por e-mail.");
            }
        } catch (AcessoNegadoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroKanban", e.getMessage());
        }

        return "redirect:/kanban?chat=" + tarefaId;
    }


    @GetMapping("/recepcao")
    public String mostrarFilaRecepcao(HttpSession session, Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        List<PedidoLigacao> pendentes = pedidoLigacaoRepository.findByStatus("PENDENTE");

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("saudacao", saudacaoAgora());
        model.addAttribute("listaDeLigacoes", pendentes);
        model.addAttribute("totalPendentes", pendentes.size());
        model.addAttribute("listaAgendamentos", agendamentoService.listarOcupacaoAtiva());
        model.addAttribute("listaLigacoesConcluidas", pedidoLigacaoService.listarConcluidasHoje());
        adicionarResumoTarefas(model, usuarioLogado);

        return "fila-chamados";
    }

    @PostMapping("/concluir-ligacao/{id}")
    public String concluirLigacao(@PathVariable Long id,
                                  @RequestParam(required = false) String resultado,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        try {
            pedidoLigacaoService.concluirSePendente(id, resultado);
            redirectAttributes.addFlashAttribute("sucessoAgendamento", "Ligação concluída.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAgendamento", e.getMessage());
        }
        return "redirect:/recepcao";
    }


    @GetMapping("/usuario")
    public String mostrarPortalUsuario(HttpSession session, Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        List<PedidoLigacao> minhasLigacoes = new ArrayList<>(pedidoLigacaoService.listarMinhas(usuarioLogado));
        minhasLigacoes.removeIf(l -> l.getDestinatario() == null || l.getDestinatario().isBlank());

        long pendentes = pedidoLigacaoService.contarMinhasPendentes(usuarioLogado);

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("saudacao", saudacaoAgora());
        model.addAttribute("listaDeSalas", salaService.listarDisponiveis());
        model.addAttribute("listaAgenda", contatoAgendaService.listarAtivos());
        model.addAttribute("listaAgendamentos", agendamentoService.listarOcupacaoAtiva());
        model.addAttribute("minhasLigacoes", minhasLigacoes);
        model.addAttribute("minhasLigacoesPendentes", pendentes);
        model.addAttribute("minhasReservasHoje", agendamentoService.listarMinhasDoDia(usuarioLogado));
        adicionarResumoTarefas(model, usuarioLogado);

        return "portal-usuario";
    }

    @PostMapping("/fazer-pedido-ligacao")
    public String salvarPedidoLigacao(@ModelAttribute PedidoLigacaoForm form,
                                      BindingResult bindingResult,
                                      HttpServletRequest request,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        UsuarioSessao logado = getUsuarioLogado(session);
        if (logado == null) return "redirect:/";

        if (!mutationRateLimiter.allowCriarLigacao(chaveRateLimit(request, logado.getEmail()))) {
            redirectAttributes.addFlashAttribute("erroAgendamento", "Muitas solicitações. Aguarde alguns minutos e tente novamente.");
            return "redirect:" + rotaPorPerfil(logado.getPerfil());
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("erroAgendamento", "Não foi possível registrar o pedido. Verifique os dados e tente novamente.");
            return "redirect:" + rotaPorPerfil(logado.getPerfil());
        }

        try {
            pedidoLigacaoService.criar(form, logado);
            redirectAttributes.addFlashAttribute("sucessoAgendamento", "Pedido de ligação enviado.");
        } catch (AcessoNegadoException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAgendamento", e.getMessage());
        }
        return "redirect:" + rotaPorPerfil(logado.getPerfil());
    }


    @PostMapping("/fazer-agendamento")
    public String fazerAgendamento(@ModelAttribute Agendamento novoAgendamento,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        UsuarioSessao logado = getUsuarioLogado(session);
        if (logado == null) return "redirect:/";

        String voltaPara = rotaPorPerfil(logado.getPerfil());

        try {
            agendamentoService.validarESalvar(novoAgendamento, logado);
            redirectAttributes.addFlashAttribute("sucessoAgendamento", "Sala reservada com sucesso!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAgendamento", e.getMessage());
        }

        return "redirect:" + voltaPara;
    }


    @GetMapping("/admin")
    public String mostrarPortalAdmin(HttpSession session, Model model) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) return "redirect:/";

        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("saudacao", saudacaoAgora());
        model.addAttribute("qtdAFazer", tarefaRepository.countByStatus("A_FAZER"));
        model.addAttribute("qtdFazendo", tarefaRepository.countByStatus("FAZENDO"));
        model.addAttribute("qtdConcluido", tarefaRepository.countByStatus("CONCLUIDO"));
        List<Usuario> listaUsuarios = usuarioRepository.findAll().stream()
                .filter(Usuario::isAtivo)
                .toList();
        model.addAttribute("qtdUsuarios", listaUsuarios.size());
        model.addAttribute("listaDeSalas", salaService.listarDisponiveis());
        model.addAttribute("listaAgenda", contatoAgendaService.listarAtivos());
        model.addAttribute("listaUsuarios", listaUsuarios);
        model.addAttribute("ultimasTarefas", tarefaRepository.findTop8ByOrderByDataCriacaoDesc());

        return "portal-admin";
    }

    @PostMapping("/cadastrar-sala")
    public String cadastrarNovaSala(@Valid @ModelAttribute SalaForm form,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("erroAdmin",
                    primeiraMensagem(bindingResult, "Não foi possível cadastrar a sala. Verifique os dados e tente novamente."));
            return "redirect:/admin";
        }
        try {
            salaService.criar(form);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Sala cadastrada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/cadastrar-contato")
    public String cadastrarNovoContato(@Valid @ModelAttribute ContatoAgendaForm form,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("erroAdmin",
                    primeiraMensagem(bindingResult, "Não foi possível cadastrar o contato. Verifique os dados e tente novamente."));
            return "redirect:/admin";
        }
        try {
            contatoAgendaService.criar(form);
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Contato cadastrado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/cadastrar-usuario")
    public String cadastrarNovoUsuario(@Valid @ModelAttribute UsuarioForm form,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("erroAdmin",
                    primeiraMensagem(bindingResult, "Não foi possível cadastrar o usuário. Verifique os dados e tente novamente."));
            return "redirect:/admin";
        }
        try {
            Usuario criado = usuarioService.cadastrar(form);
            log.info("Usuário cadastrado: {} ({})", criado.getEmail(), criado.getPerfil());
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Usuário cadastrado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/deletar-usuario/{id}")
    public String deletarUsuario(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null || usuarioLogado.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("erroAdmin", "Você não pode desativar o próprio acesso.");
            return "redirect:/admin";
        }
        try {
            usuarioService.desativar(id);
            log.info("Usuário {} desativado por {}", id, usuarioLogado.getEmail());
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Usuário desativado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/redefinir-senha/{id}")
    public String redefinirSenha(@PathVariable Long id,
                                 @RequestParam String novaSenha,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UsuarioSessao admin = getUsuarioLogado(session);
        try {
            usuarioService.redefinirSenha(id, novaSenha);
            if (admin != null) {
                log.info("Senha redefinida do usuário {} por {}", id, admin.getEmail());
            }
            redirectAttributes.addFlashAttribute("sucessoAdmin", "Senha atualizada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroAdmin", e.getMessage());
        }
        return "redirect:/admin";
    }

    @GetMapping("/admin/exportar-relatorio")
    public ResponseEntity<byte[]> exportarRelatorioExcel(HttpSession session) {
        UsuarioSessao usuarioLogado = getUsuarioLogado(session);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            excelExportService.exportarRelatorio(usuarioLogado, buffer);
        } catch (AcessoNegadoException e) {
            log.warn("Exportação de relatório recusada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            log.error("Falha ao gerar relatório Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        byte[] arquivo = buffer.toByteArray();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExcelExportService.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + ExcelExportService.FILENAME
                                + "\"; filename*=UTF-8''" + ExcelExportService.FILENAME)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(arquivo.length)
                .body(arquivo);
    }

    private String montarCorpoEmailHistorico(Tarefa tarefa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá!\n\nAqui está a cópia oficial do histórico do chamado.\n\n");
        sb.append(" TAREFA #").append(tarefa.getId()).append(" · ").append(tarefa.getTitulo()).append("\n");
        sb.append("Descrição: ").append(tarefa.getDescricao()).append("\n");
        sb.append("Status Atual: ").append(tarefa.getStatus()).append("\n\n");
        sb.append("HISTÓRICO DE CONVERSA:\n");

        if (tarefa.getComentarios() == null || tarefa.getComentarios().isEmpty()) {
            sb.append("Nenhum comentário registrado.\n");
        } else {
            for (Comentario c : tarefa.getComentarios()) {
                sb.append(" > ").append(c.getAutor()).append(": ").append(c.getTexto()).append("\n");
            }
        }
        sb.append("\n\nEnviado via Sistema SeOrganize");
        return sb.toString();
    }

    private Set<String> destinatariosHistorico(Tarefa tarefa) {
        Set<String> destinatarios = new LinkedHashSet<>();
        if (tarefa.getResponsaveis() != null) {
            for (Usuario r : tarefa.getResponsaveis()) {
                if (r != null && r.getEmail() != null && !r.getEmail().isBlank()) {
                    destinatarios.add(r.getEmail());
                }
            }
        }
        if (tarefa.getSolicitanteId() != null) {
            usuarioRepository.findById(tarefa.getSolicitanteId())
                    .map(Usuario::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .ifPresent(destinatarios::add);
        } else if (tarefa.getSolicitante() != null && !tarefa.getSolicitante().isBlank()) {
            usuarioRepository.findAll().stream()
                    .filter(u -> tarefa.getSolicitante().equals(u.getNome()))
                    .map(Usuario::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .findFirst()
                    .ifPresent(destinatarios::add);
        }
        return destinatarios;
    }

    private static String primeiraMensagem(BindingResult bindingResult, String fallback) {
        if (bindingResult != null && bindingResult.getFieldError() != null
                && bindingResult.getFieldError().getDefaultMessage() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }
        return fallback;
    }
}
