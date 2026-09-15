package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.dto.ContatoAgendaForm;
import com.example.Sla_Reuniao.model.ContatoAgenda;
import com.example.Sla_Reuniao.repository.ContatoAgendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContatoAgendaService {

    private static final int NOME_MAX = 255;
    private static final int TELEFONE_MAX = 40;
    private static final int DEPARTAMENTO_MAX = 255;

    private final ContatoAgendaRepository contatoAgendaRepository;

    public ContatoAgendaService(ContatoAgendaRepository contatoAgendaRepository) {
        this.contatoAgendaRepository = contatoAgendaRepository;
    }

    @Transactional(readOnly = true)
    public ContatoAgenda buscarOuFalhar(Long id) {
        return contatoAgendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contato não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<ContatoAgenda> listarAtivos() {
        return contatoAgendaRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional
    public ContatoAgenda criar(ContatoAgendaForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Informe os dados do contato.");
        }
        ContatoAgenda contato = new ContatoAgenda();
        contato.setNome(obrigatorio(form.getNome(), "O nome do contacto ou empresa é obrigatório", NOME_MAX));
        contato.setTelefone(obrigatorio(form.getTelefone(), "O número de telefone é obrigatório para a agenda", TELEFONE_MAX));
        contato.setDepartamento(opcional(form.getDepartamento(), DEPARTAMENTO_MAX));
        contato.setAtivo(true);
        return contatoAgendaRepository.save(contato);
    }

    @Transactional
    public ContatoAgenda atualizar(Long id, String nome, String telefone, String departamento) {
        ContatoAgenda contato = buscarOuFalhar(id);
        contato.setNome(obrigatorio(nome, "O nome do contacto ou empresa é obrigatório", NOME_MAX));
        contato.setTelefone(obrigatorio(telefone, "O número de telefone é obrigatório para a agenda", TELEFONE_MAX));
        contato.setDepartamento(opcional(departamento, DEPARTAMENTO_MAX));
        return contatoAgendaRepository.save(contato);
    }

    @Transactional
    public ContatoAgenda arquivar(Long id) {
        ContatoAgenda contato = buscarOuFalhar(id);
        contato.setAtivo(false);
        return contatoAgendaRepository.save(contato);
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
