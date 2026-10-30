package com.example.Sla_Reuniao.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entrega o build do painel React, que fica em static/painel. */
@Controller
public class PainelPaginaController {

    @GetMapping({"/painel", "/painel/"})
    public String painel() {
        return "forward:/painel/index.html";
    }
}
