package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.model.Chamado;
import com.example.Sla_Reuniao.repository.ChamadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoRepository chamadoRepository;

    public ChamadoController(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    @PostMapping
    public ResponseEntity<Chamado> registrarChamadoRecepcao(@Valid @RequestBody Chamado chamadoRecebido) {
        try {
            Chamado chamadoSalvo = chamadoRepository.save(chamadoRecebido);
            return ResponseEntity.status(HttpStatus.CREATED).body(chamadoSalvo);
        } catch (Exception excecaoPersistencia) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/fila")
    public ResponseEntity<List<Chamado>> consultarFilaAtendimento() {
        try {
            List<Chamado> filaAtual = chamadoRepository.findAll();
            if (filaAtual.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(filaAtual);
        } catch (Exception excecaoConsulta) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("/{id}/concluir")
    public ResponseEntity<Chamado> concluirChamado(@PathVariable Long id) {
        try {

            Optional<Chamado> chamadoExistente = chamadoRepository.findById(id);


            if (chamadoExistente.isEmpty()) {
                return ResponseEntity.notFound().build();
            }


            Chamado chamado = chamadoExistente.get();
            chamado.setStatus(Chamado.StatusChamado.CONCLUIDO);

            Chamado chamadoAtualizado = chamadoRepository.save(chamado);
            return ResponseEntity.ok(chamadoAtualizado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}