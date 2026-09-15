package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.dto.SalaForm;
import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salaRepository;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    public Sala buscarOuFalhar(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sala não encontrada."));
    }

    public List<Sala> listarDisponiveis() {
        return salaRepository.findByDisponivelTrueOrderByNomeAsc();
    }

    @Transactional
    public Sala criar(SalaForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Informe os dados da sala.");
        }
        Sala sala = new Sala();
        sala.setNome(obrigatorio(form.getNome(), "O nome ou identificador da sala é obrigatório"));
        if (form.getCapacidade() == null || form.getCapacidade() < 1) {
            throw new IllegalArgumentException("A capacidade mínima deve ser de pelo menos 1 pessoa");
        }
        sala.setCapacidade(form.getCapacidade());
        sala.setRecursos(vazioParaNulo(form.getRecursos()));
        sala.setLocalizacao(obrigatorio(form.getLocalizacao(), "A localização é obrigatória"));
        sala.setDisponivel(true);
        return salaRepository.save(sala);
    }

    @Transactional
    public Sala atualizar(Long id, String nome, Integer capacidade, String recursos, String localizacao) {
        Sala sala = buscarOuFalhar(id);
        sala.setNome(obrigatorio(nome, "O nome ou identificador da sala é obrigatório"));
        if (capacidade == null || capacidade < 1) {
            throw new IllegalArgumentException("A capacidade mínima deve ser de pelo menos 1 pessoa");
        }
        sala.setCapacidade(capacidade);
        sala.setRecursos(vazioParaNulo(recursos));
        sala.setLocalizacao(obrigatorio(localizacao, "A localização é obrigatória"));
        return salaRepository.save(sala);
    }

    @Transactional
    public Sala arquivar(Long id) {
        Sala sala = buscarOuFalhar(id);
        sala.setDisponivel(false);
        return salaRepository.save(sala);
    }

    private String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private String vazioParaNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
