package com.example.Sla_Reuniao.config;

import com.example.Sla_Reuniao.model.Comentario;
import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.TarefaRepository;
import com.example.Sla_Reuniao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Quadro de exemplo só no H2 local, para encher o Kanban sem tocar em produção. */
@Component
@Profile("local")
@Order(2)
public class LocalKanbanSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalKanbanSeeder.class);
    private static final long MIN_TAREFAS_PARA_PULAR = 20;

    private final Environment environment;
    private final DataSource dataSource;
    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalKanbanSeeder(Environment environment,
                             DataSource dataSource,
                             TarefaRepository tarefaRepository,
                             UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!podePopular()) {
            return;
        }
        if (tarefaRepository.count() >= MIN_TAREFAS_PARA_PULAR) {
            log.info("Kanban local já tem tarefas, seed ignorado.");
            return;
        }

        List<Usuario> equipe = garantirUsuariosDemo();
        LocalDateTime agora = LocalDateTime.now();
        int criadas = 0;

        for (SeedItem item : catalogo()) {
            Tarefa tarefa = new Tarefa();
            tarefa.setTitulo(item.titulo);
            tarefa.setDescricao(item.descricao);
            tarefa.setStatus(item.status);
            tarefa.setDataCriacao(agora.minusHours(item.horasAtras));

            Usuario solicitante = equipe.get(item.solicitanteIdx);
            tarefa.setSolicitante(solicitante.getNome());
            tarefa.setSolicitanteId(solicitante.getId());

            if (item.responsavelIdx.length > 0) {
                List<Usuario> responsaveis = new ArrayList<>();
                for (int idx : item.responsavelIdx) {
                    responsaveis.add(equipe.get(idx));
                }
                tarefa.setResponsaveis(responsaveis);
            }

            for (int i = 0; i < item.comentarios.length; i++) {
                Comentario comentario = new Comentario();
                comentario.setTexto(item.comentarios[i]);
                comentario.setAutor(equipe.get(item.autorComentarioIdx[i]).getNome());
                comentario.setDataCriacao(agora.minusHours(item.horasAtras).plusMinutes(20L * (i + 1)));
                comentario.setTarefa(tarefa);
                tarefa.getComentarios().add(comentario);
            }

            tarefaRepository.save(tarefa);
            criadas++;
        }

        log.info("Kanban local: {} solicitações de demonstração criadas (H2).", criadas);
    }

    private boolean podePopular() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("LocalKanbanSeeder ignorado: perfil prod ativo.");
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            if (url == null || !url.toLowerCase().contains("jdbc:h2")) {
                log.warn("LocalKanbanSeeder ignorado: datasource não é H2 ({}).", url);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("LocalKanbanSeeder ignorado: não foi possível inspecionar o datasource.", ex);
            return false;
        }
    }

    private List<Usuario> garantirUsuariosDemo() {
        List<Usuario> equipe = new ArrayList<>();
        equipe.add(usuarioOuCria("admin@local", "Admin local", "ADMIN", "admin1234"));
        equipe.add(usuarioOuCria("ana@local", "Ana Costa", "COLABORADOR", "admin1234"));
        equipe.add(usuarioOuCria("bruno@local", "Bruno Lima", "COLABORADOR", "admin1234"));
        equipe.add(usuarioOuCria("carla@local", "Carla Souza", "RECEPCAO", "admin1234"));
        equipe.add(usuarioOuCria("diego@local", "Diego Alves", "COLABORADOR", "admin1234"));
        return equipe;
    }

    private Usuario usuarioOuCria(String email, String nome, String perfil, String senha) {
        Usuario existente = usuarioRepository.findByEmail(email);
        if (existente != null) {
            return existente;
        }
        Usuario novo = new Usuario();
        novo.setNome(nome);
        novo.setEmail(email);
        novo.setSenha(passwordEncoder.encode(senha));
        novo.setPerfil(perfil);
        return usuarioRepository.save(novo);
    }

    private List<SeedItem> catalogo() {
        return List.of(
                aFazer("Reset de senha no e-mail corporativo", "Usuário não consegue entrar no Outlook após troca de senha.", 4, 1, 2, "Senha expirada hoje de manhã.", "Pode resetar e avisar a Ana?"),
                aFazer("Impressora 3º andar sem toner", "HP LaserJet do copa-café está piscando toner vazio.", 3, 3, 4),
                aFazer("VPN cai a cada 10 minutos", "Home office: túnel OpenVPN desconecta sozinho.", 12, 2, 1, "Já tentei outro provedor, mesmo sintoma."),
                aFazer("Notebook não liga na sala 214", "Dell Latitude fica na tela preta após o logo.", 2, 4, 4),
                aFazer("Acesso ao drive compartilhado RH", "Pasta \\\\fileserver\\rh-folha não abre para a nova analista.", 7, 1, 1),
                aFazer("Mouse e teclado USB com falha", "Estação da recepção perde o cursor aleatoriamente.", 5, 3, 2),
                aFazer("Instalar LibreOffice na sala de treinamento", "8 PCs ainda estão com a suíte antiga.", 20, 4, 4),
                aFazer("Câmera da sala de reunião não aparece no Teams", "Dispositivo some da lista depois do último Windows Update.", 9, 2, 1, "Já reinstalei o cliente Teams."),
                aFazer("Criar usuário para estagiário de TI", "Início na segunda: precisa de e-mail, VPN e crachá lógico.", 1, 0, 0),
                aFazer("Monitor piscando na contabilidade", "Dell 24\" com faixas horizontais intermitentes.", 14, 1, 2),
                aFazer("Wi-Fi lento no auditório", "Palestra amanhã: 40 convidados, sinal some no fundo da sala.", 6, 4, 4),
                aFazer("Atualizar antivírus nas estações do 2º", "12 máquinas ainda na versão 12.4.", 28, 2, 2),
                aFazer("Telefone IP da diretoria sem ramal", "Aparelho Yealink mostra 'register failed'.", 3, 3, 1),
                aFazer("Permissão no sistema de ponto", "Colaborador novo não bate ponto pelo app.", 8, 1, 1),
                aFazer("Trocar HD do arquivo morto", "SMART acusando reallocated sectors no NAS antigo.", 36, 4, 4),
                aFazer("Projetor da sala 3 não dá imagem HDMI", "VGA funciona, HDMI não. Cabo novo já testado.", 11, 2, 2),
                aFazer("Desbloquear USB na estação do financeiro", "Política GPO bloqueou pen-drive do contador externo.", 4, 1, 0),
                aFazer("Configurar assinatura de e-mail padronizada", "Jurídico pediu bloco com CNPJ e endereço novos.", 16, 3, 3),
                aFazer("Falha ao abrir planilha de 80 MB", "Excel trava em 'calculando' no PC da controladoria.", 10, 2, 4),
                aFazer("Crachá RFID não abre a catraca", "Usuário Carla: o leitor bipa mas não libera.", 2, 3, 1),
                aFazer("Backup da pasta de projetos atrasado", "Job noturno do sexta falhou com disco cheio.", 22, 4, 4, "Disco de destino com 98% de uso."),
                aFazer("Instalar leitor biométrico na portaria", "Equipamento já chegou; falta driver e cadastro.", 18, 0, -1),

                fazendo("Troca de switch do 1º andar", "Switch 24p com 3 portas mortas; peça no estoque.", 30, 4, 4, "Peça conferida. Janela às 18h.", "Comunicar recepção antes de derrubar a rede."),
                fazendo("Migração de caixa de e-mail do jurídico", "Mailbox de 18 GB para o novo tenant.", 40, 1, 1, "PST antigo já exportado."),
                fazendo("Reparo no ar-condicionado do datacenter", "Temperatura subiu a 27 °C; chamado aberto com o predial.", 8, 0, 4, "Técnico previsto para 14h."),
                fazendo("Inventário de licenças do Office", "Cruzar 62 estações com o portal Microsoft 365.", 50, 2, 2),
                fazendo("Ajuste de firewall para o novo ERP", "Liberar 443/8443 só da VLAN administrativa.", 15, 4, 1, "Regra de homologação já aplicada.", "Falta produção e documentação."),
                fazendo("Recuperar arquivos apagados da pasta Marketing", "Delete acidental na sexta; snapshot existe.", 6, 1, 4, "Snapshot de sexta 22h localizado."),
                fazendo("Padronizar imagem Windows 11", "Novo golden image com BitLocker e agentes internos.", 60, 2, 2),
                fazendo("Trocar fonte da workstation de edição", "PC da comunicação reinicia sob carga.", 12, 4, 4),
                fazendo("Treinamento de phishing da turma 2", "Agendar sala e lista de presença com RH.", 9, 3, 3, "RH confirmou 18 participantes."),
                fazendo("Configurar impressão frente-e-verso padrão", "Política de economia de papel no 2º e 3º andares.", 25, 2, 0),
                fazendo("Monitoramento do link secundário", "Failover não disparou no teste de ontem.", 5, 4, 4, "Ticket aberto com a operadora."),
                fazendo("Atualizar firmware dos APs", "6 access points Unifi ainda na 6.0.", 33, 4, 2),
                fazendo("Criar grupo no Teams para o comitê LGPD", "Incluir jurídico, TI e DPO.", 7, 1, 1),
                fazendo("Limpeza de tickets órfãos no Kanban antigo", "Exportar CSV e arquivar o quadro legado.", 45, 0, 0),
                fazendo("Homologar scanner de documentos", "Fujitsu fi-7160 na sala do protocolo.", 19, 3, 2),
                fazendo("Ajuste de quota de e-mail da diretoria", "Caixa em 99%; habilitar arquivo morto.", 3, 1, 1, "Usuária avisada para não apagar itens."),

                concluido("Reset de senha do Wi-Fi visitante", "Senha trimestral renovada e cartaz atualizado.", 72, 3, 3, "Cartaz impresso e colocado na recepção."),
                concluido("Instalar webcam na sala 1", "Logitech C920 testada em reunião com o conselho.", 90, 2, 2),
                concluido("Liberar acesso ao Ponto Digital", "App e web para 4 admitidos de setembro.", 80, 1, 1, "RH confirmou batidas de ontem."),
                concluido("Troca de teclado da recepção", "Teclado novo ABNT2 instalado.", 100, 3, 4),
                concluido("Corrigir DNS interno do ERP", "Registro apontava para o servidor antigo.", 88, 4, 4, "nslookup ok em todas as VLANs."),
                concluido("Configurar 2FA no e-mail da diretoria", "Microsoft Authenticator ativado.", 110, 0, 1, "Teste de login ok com a Ana."),
                concluido("Limpeza física do rack", "Removidos patch cords soltos e etiquetas novas.", 130, 4, 4),
                concluido("Criar lista de distribuição Compras", "compras@local com 6 membros.", 95, 1, 1),
                concluido("Atualizar Java nas estações do financeiro", "Temurin 21 nas 9 máquinas.", 140, 2, 2, "Folha de pagamento rodou sem erro."),
                concluido("Reparar cabo de rede da sala 108", "Crimpagem nova; link a 1 Gbps.", 76, 4, 4),
                concluido("Desligar PC ocioso do arquivo", "Equipamento formatado e enviado ao estoque.", 160, 0, 2),
                concluido("Ajuste de brilho dos monitores da ouvidoria", "Perfil sRGB e escala 125%.", 84, 1, 1),
                concluido("Renovar certificado do painel interno", "Let's Encrypt renovado; nginx recarregado.", 70, 4, 0, "HTTPS verde no painel."),
                concluido("Mapear impressora jurídica no 4º", "Fila 'JUR-HP' publicada no AD.", 120, 2, 3)
        );
    }

    private SeedItem aFazer(String titulo, String descricao, int horas, int solicitante, int responsavel, String... comentarios) {
        return item("A_FAZER", titulo, descricao, horas, solicitante, responsavel, comentarios);
    }

    private SeedItem fazendo(String titulo, String descricao, int horas, int solicitante, int responsavel, String... comentarios) {
        return item("FAZENDO", titulo, descricao, horas, solicitante, responsavel, comentarios);
    }

    private SeedItem concluido(String titulo, String descricao, int horas, int solicitante, int responsavel, String... comentarios) {
        return item("CONCLUIDO", titulo, descricao, horas, solicitante, responsavel, comentarios);
    }

    private SeedItem item(String status, String titulo, String descricao, int horasAtras,
                          int solicitanteIdx, int responsavelIdx, String... comentarios) {
        int[] responsaveis = responsavelIdx < 0 ? new int[0] : new int[]{responsavelIdx};
        int[] autores = new int[comentarios.length];
        for (int i = 0; i < comentarios.length; i++) {
            autores[i] = i % 2 == 0 ? solicitanteIdx : Math.max(responsavelIdx, 0);
        }
        return new SeedItem(titulo, descricao, status, horasAtras, solicitanteIdx, responsaveis, comentarios, autores);
    }

    private record SeedItem(String titulo,
                            String descricao,
                            String status,
                            int horasAtras,
                            int solicitanteIdx,
                            int[] responsavelIdx,
                            String[] comentarios,
                            int[] autorComentarioIdx) {
    }
}
