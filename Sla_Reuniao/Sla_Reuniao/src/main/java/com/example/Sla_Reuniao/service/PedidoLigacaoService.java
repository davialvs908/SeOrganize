package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.dto.PedidoLigacaoForm;
import com.example.Sla_Reuniao.model.ContatoAgenda;
import com.example.Sla_Reuniao.model.PedidoLigacao;
import com.example.Sla_Reuniao.repository.PedidoLigacaoRepository;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class PedidoLigacaoService {

    private static final Set<String> URGENCIAS_VALIDAS = Set.of("BAIXA", "NORMAL", "ALTA");

    private final PedidoLigacaoRepository pedidoLigacaoRepository;
    private final ContatoAgendaService contatoAgendaService;

    public PedidoLigacaoService(PedidoLigacaoRepository pedidoLigacaoRepository,
                                ContatoAgendaService contatoAgendaService) {
        this.pedidoLigacaoRepository = pedidoLigacaoRepository;
        this.contatoAgendaService = contatoAgendaService;
    }

    @Transactional
    public PedidoLigacao criar(PedidoLigacaoForm form, UsuarioSessao session) {
        if (session == null || session.getId() == null) {
            throw new AcessoNegadoException("É necessário estar autenticado para pedir uma ligação.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Informe os dados do pedido de ligação.");
        }

        PedidoLigacao pedido = new PedidoLigacao();
        pedido.setId(null);
        pedido.setStatus("PENDENTE");
        pedido.setResultado(null);
        pedido.setSolicitanteId(session.getId());
        pedido.setNomeColaborador(session.getNome());
        pedido.setDestinatario(obrigatorio(form.getDestinatario(), "Para quem a recepção deve ligar?", 255));
        pedido.setTelefone(opcional(form.getTelefone(), 40));
        pedido.setMotivoLigacao(obrigatorio(form.getMotivoLigacao(), "O motivo da ligação é obrigatório", 500));
        pedido.setUrgencia(normalizarUrgencia(form.getUrgencia()));

        if (form.getContatoAgendaId() != null) {
            ContatoAgenda contato = contatoAgendaService.buscarOuFalhar(form.getContatoAgendaId());
            pedido.setContatoAgenda(contato);
        } else {
            pedido.setContatoAgenda(null);
        }

        return pedidoLigacaoRepository.save(pedido);
    }

    @Transactional
    public PedidoLigacao concluirSePendente(Long id, String resultado) {
        PedidoLigacao pedido = buscarOuFalhar(id);
        if (!"PENDENTE".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Só é possível concluir um pedido pendente.");
        }
        pedido.setStatus("CONCLUIDO");
        if (resultado != null && !resultado.isBlank()) {
            String normalizado = resultado.trim();
            if (normalizado.length() > 20) {
                throw new IllegalArgumentException("O resultado não pode ter mais de 20 caracteres.");
            }
            pedido.setResultado(normalizado);
        }
        return pedidoLigacaoRepository.save(pedido);
    }

    public PedidoLigacao garantirAcesso(Long id, UsuarioSessao usuario) {
        PedidoLigacao pedido = buscarOuFalhar(id);
        if (!ehDono(pedido, usuario) && !ehRecepcaoOuAdmin(usuario)) {
            throw new AcessoNegadoException("Você não tem permissão para este pedido de ligação.");
        }
        return pedido;
    }

    @Transactional
    public PedidoLigacao cancelarSePendente(Long id, UsuarioSessao session) {
        PedidoLigacao pedido = buscarOuFalhar(id);
        if (!ehDono(pedido, session)) {
            throw new AcessoNegadoException("Apenas o solicitante pode cancelar este pedido.");
        }
        if (!"PENDENTE".equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Só é possível cancelar um pedido pendente.");
        }
        pedido.setStatus("CANCELADO");
        return pedidoLigacaoRepository.save(pedido);
    }

    public List<PedidoLigacao> listarMinhas(UsuarioSessao session) {
        if (session == null || session.getId() == null) {
            return List.of();
        }
        return pedidoLigacaoRepository.findBySolicitanteIdOrderByDataSolicitacaoDesc(session.getId());
    }

    public long contarMinhasPendentes(UsuarioSessao session) {
        if (session == null || session.getId() == null) {
            return 0L;
        }
        return pedidoLigacaoRepository.countBySolicitanteIdAndStatus(session.getId(), "PENDENTE");
    }

    public List<PedidoLigacao> listarPendentesRecepcao() {
        return pedidoLigacaoRepository.findByStatus("PENDENTE");
    }

    public List<PedidoLigacao> listarConcluidasHoje() {
        return pedidoLigacaoRepository.findByStatusAndDataSolicitacaoAfter(
                "CONCLUIDO", LocalDate.now().atStartOfDay());
    }

    private PedidoLigacao buscarOuFalhar(Long id) {
        return pedidoLigacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de ligação não encontrado."));
    }

    private boolean ehDono(PedidoLigacao pedido, UsuarioSessao usuario) {
        return pedido != null
                && usuario != null
                && usuario.getId() != null
                && usuario.getId().equals(pedido.getSolicitanteId());
    }

    private boolean ehRecepcaoOuAdmin(UsuarioSessao usuario) {
        if (usuario == null) {
            return false;
        }
        return usuario.isAdmin() || "RECEPCAO".equals(usuario.getPerfil());
    }

    private String normalizarUrgencia(String urgencia) {
        if (urgencia == null || urgencia.isBlank()) {
            return "NORMAL";
        }
        String normalizada = urgencia.trim().toUpperCase();
        if (!URGENCIAS_VALIDAS.contains(normalizada)) {
            throw new IllegalArgumentException("Urgência inválida.");
        }
        return normalizada;
    }

    private String obrigatorio(String valor, String mensagem, int max) {
        String normalizado = opcional(valor, max);
        if (normalizado == null) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private String opcional(String valor, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() > max) {
            throw new IllegalArgumentException("O texto não pode ter mais de " + max + " caracteres.");
        }
        return normalizado;
    }
}
