package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Comentario;
import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.TarefaRepository;
import com.example.Sla_Reuniao.repository.UsuarioRepository;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TarefaService {

    public static final Set<String> STATUS_VALIDOS = Set.of("A_FAZER", "FAZENDO", "CONCLUIDO");
    public static final Set<String> PERFIS_VALIDOS = Set.of("ADMIN", "RECEPCAO", "COLABORADOR");
    public static final int COMENTARIO_MAX = 2000;

    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public TarefaService(TarefaRepository tarefaRepository,
                         UsuarioRepository usuarioRepository,
                         EmailService emailService) {
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    public Tarefa buscarOuFalhar(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada."));
    }

    public boolean podeAcessar(Tarefa tarefa, UsuarioSessao usuario) {
        if (tarefa == null || usuario == null || usuario.getId() == null) {
            return false;
        }
        if (usuario.isAdmin()) {
            return true;
        }
        if (usuario.getId().equals(tarefa.getSolicitanteId())) {
            return true;
        }
        return ehResponsavel(tarefa, usuario.getId());
    }

    public boolean podeExcluir(Tarefa tarefa, UsuarioSessao usuario) {
        if (tarefa == null || usuario == null || usuario.getId() == null) {
            return false;
        }
        if (usuario.isAdmin()) {
            return true;
        }
        return usuario.getId().equals(tarefa.getSolicitanteId());
    }

    public void garantirAcesso(Tarefa tarefa, UsuarioSessao usuario) {
        if (!podeAcessar(tarefa, usuario)) {
            throw new AcessoNegadoException("Você não tem permissão para esta tarefa.");
        }
    }

    public List<Tarefa> listarPorStatus(String status, UsuarioSessao usuario) {
        validarStatus(status);
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        if (usuario.isAdmin()) {
            return tarefaRepository.findByStatus(status);
        }
        return tarefaRepository.findByStatusAndUsuarioEnvolvido(status, usuario.getId());
    }

    public List<Tarefa> listarDoUsuario(UsuarioSessao usuario) {
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        if (usuario.isAdmin()) {
            return tarefaRepository.findAll();
        }
        return tarefaRepository.findAllByUsuarioEnvolvido(usuario.getId());
    }

    public long contarPorStatus(String status, UsuarioSessao usuario) {
        validarStatus(status);
        if (usuario == null || usuario.getId() == null) {
            return 0;
        }
        if (usuario.isAdmin()) {
            return tarefaRepository.countByStatus(status);
        }
        return tarefaRepository.findByStatusAndUsuarioEnvolvido(status, usuario.getId()).size();
    }

    @Transactional
    public Tarefa criar(String titulo, String descricao, List<Long> responsaveisIds, UsuarioSessao autor) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setSolicitante(autor.getNome());
        tarefa.setSolicitanteId(autor.getId());
        tarefa.setResponsaveis(carregarResponsaveis(responsaveisIds));
        Tarefa salva = tarefaRepository.save(tarefa);
        notificarNovosResponsaveis(salva, Set.of());
        return salva;
    }

    @Transactional
    public void moverTarefa(Long id, String novoStatus, UsuarioSessao usuario) {
        validarStatus(novoStatus);
        Tarefa tarefa = buscarOuFalhar(id);
        garantirAcesso(tarefa, usuario);
        tarefa.setStatus(novoStatus);
        tarefaRepository.save(tarefa);
    }

    @Transactional
    public void editar(Long id, String titulo, String descricao, List<Long> responsaveisIds, UsuarioSessao usuario) {
        Tarefa tarefa = buscarOuFalhar(id);
        garantirAcesso(tarefa, usuario);
        Set<Long> responsaveisAnteriores = idsResponsaveis(tarefa);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setResponsaveis(carregarResponsaveis(responsaveisIds));
        tarefaRepository.save(tarefa);
        notificarNovosResponsaveis(tarefa, responsaveisAnteriores);
    }

    @Transactional
    public void excluir(Long id, UsuarioSessao usuario) {
        Tarefa tarefa = buscarOuFalhar(id);
        if (!podeExcluir(tarefa, usuario)) {
            throw new AcessoNegadoException("Apenas o solicitante ou um administrador pode excluir esta tarefa.");
        }
        tarefaRepository.delete(tarefa);
    }

    @Transactional
    public void adicionarComentario(Long tarefaId, String texto, UsuarioSessao usuario) {
        String textoNormalizado = validarTextoComentario(texto);
        Tarefa tarefa = buscarOuFalhar(tarefaId);
        garantirAcesso(tarefa, usuario);
        Comentario comentario = new Comentario();
        comentario.setTexto(textoNormalizado);
        comentario.setAutor(usuario.getNome());
        comentario.setTarefa(tarefa);
        tarefa.getComentarios().add(comentario);
        tarefaRepository.save(tarefa);
        notificarNovoComentario(tarefa, usuario);
    }

    private static void validarStatus(String status) {
        if (status == null || !STATUS_VALIDOS.contains(status)) {
            throw new IllegalArgumentException("Status inválido.");
        }
    }

    private static String validarTextoComentario(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O comentário não pode estar em branco.");
        }
        String normalizado = texto.trim();
        if (normalizado.length() > COMENTARIO_MAX) {
            throw new IllegalArgumentException("O comentário não pode ter mais de " + COMENTARIO_MAX + " caracteres.");
        }
        return normalizado;
    }

    private static boolean ehResponsavel(Tarefa tarefa, Long usuarioId) {
        if (tarefa.getResponsaveis() == null || usuarioId == null) {
            return false;
        }
        return tarefa.getResponsaveis().stream()
                .anyMatch(r -> r != null && usuarioId.equals(r.getId()));
    }

    private List<Usuario> carregarResponsaveis(List<Long> responsaveisIds) {
        if (responsaveisIds == null || responsaveisIds.isEmpty()) {
            return new ArrayList<>();
        }
        return usuarioRepository.findAllById(responsaveisIds);
    }

    private Set<Long> idsResponsaveis(Tarefa tarefa) {
        Set<Long> ids = new HashSet<>();
        if (tarefa.getResponsaveis() == null) {
            return ids;
        }
        for (Usuario r : tarefa.getResponsaveis()) {
            if (r != null && r.getId() != null) {
                ids.add(r.getId());
            }
        }
        return ids;
    }

    private void notificarNovosResponsaveis(Tarefa tarefa, Set<Long> idsAnteriores) {
        if (tarefa.getResponsaveis() == null) {
            return;
        }
        Set<Long> anteriores = idsAnteriores == null ? Set.of() : idsAnteriores;
        Long tarefaId = tarefa.getId();
        String titulo = tarefa.getTitulo();
        List<String> emails = new ArrayList<>();
        for (Usuario r : tarefa.getResponsaveis()) {
            if (r == null || r.getId() == null || anteriores.contains(r.getId())) {
                continue;
            }
            adicionarEmail(emails, r);
        }
        if (emails.isEmpty()) {
            return;
        }
        aposCommit(() -> {
            for (String email : emails) {
                emailService.notifyResponsavelAtribuido(email, tarefaId, titulo);
            }
        });
    }

    private void notificarNovoComentario(Tarefa tarefa, UsuarioSessao autor) {
        Set<String> destinatarios = new LinkedHashSet<>();
        if (tarefa.getResponsaveis() != null) {
            for (Usuario r : tarefa.getResponsaveis()) {
                adicionarEmail(destinatarios, r);
            }
        }
        if (tarefa.getSolicitanteId() != null) {
            usuarioRepository.findById(tarefa.getSolicitanteId()).ifPresent(s -> adicionarEmail(destinatarios, s));
        }
        if (autor != null && autor.getEmail() != null) {
            destinatarios.remove(autor.getEmail());
        }
        if (destinatarios.isEmpty()) {
            return;
        }
        Long tarefaId = tarefa.getId();
        String titulo = tarefa.getTitulo();
        aposCommit(() -> {
            for (String email : destinatarios) {
                emailService.notifyNovoComentario(email, tarefaId, titulo);
            }
        });
    }

    private void aposCommit(Runnable acao) {
        if (acao == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
            return;
        }
        acao.run();
    }

    private static void adicionarEmail(List<String> destinatarios, Usuario usuario) {
        if (usuario != null && usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            destinatarios.add(usuario.getEmail());
        }
    }

    private static void adicionarEmail(Set<String> destinatarios, Usuario usuario) {
        if (usuario != null && usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            destinatarios.add(usuario.getEmail());
        }
    }
}
