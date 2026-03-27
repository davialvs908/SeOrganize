package com.example.Sla_Reuniao.controller;

import com.example.Sla_Reuniao.model.Sala;
import com.example.Sla_Reuniao.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaRepository salaRepository;

    public SalaController(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @PostMapping
    public ResponseEntity<Sala> criarSala(@Valid @RequestBody Sala novaSala) {
        try {
            Sala salaSalva = salaRepository.save(novaSala);
            return ResponseEntity.status(HttpStatus.CREATED).body(salaSalva);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Sala>> listarTodasSalas() {
        List<Sala> salas = salaRepository.findAll();
        return ResponseEntity.ok(salas);
    }
}