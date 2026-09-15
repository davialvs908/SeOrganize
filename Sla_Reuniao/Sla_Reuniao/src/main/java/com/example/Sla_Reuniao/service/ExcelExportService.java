package com.example.Sla_Reuniao.service;

import com.example.Sla_Reuniao.model.Tarefa;
import com.example.Sla_Reuniao.model.Usuario;
import com.example.Sla_Reuniao.repository.TarefaRepository;
import com.example.Sla_Reuniao.security.AcessoNegadoException;
import com.example.Sla_Reuniao.security.UsuarioSessao;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String FILENAME = "Relatorio_SeOrganize.xlsx";

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TarefaRepository tarefaRepository;

    public ExcelExportService(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    @Transactional(readOnly = true)
    public void exportarRelatorio(UsuarioSessao usuario, OutputStream destino) throws IOException {
        garantirAdmin(usuario);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.getProperties().getExtendedProperties()
                    .getUnderlyingProperties()
                    .setApplication("Microsoft Excel");
            preencherTarefas(workbook);
            preencherResumoStatus(workbook);
            workbook.write(destino);
        }
    }

    private void preencherTarefas(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Resumo de Tarefas");
        CellStyle estiloTexto = criarEstiloTexto(workbook);
        criarCabecalho(sheet, "ID", "Título", "Descrição", "Solicitante", "Responsáveis", "Status", "Data de criação", "Comentários");

        int row = 1;
        for (Tarefa t : tarefaRepository.findAll()) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(t.getId() == null ? 0 : t.getId());
            escreverTexto(r, 1, t.getTitulo(), estiloTexto);
            escreverTexto(r, 2, t.getDescricao(), estiloTexto);
            escreverTexto(r, 3, t.getSolicitante(), estiloTexto);
            escreverTexto(r, 4, nomesResponsaveis(t), estiloTexto);
            escreverTexto(r, 5, t.getStatus(), estiloTexto);
            escreverTexto(r, 6, t.getDataCriacao() == null ? "" : t.getDataCriacao().format(DATA_BR), estiloTexto);
            r.createCell(7).setCellValue(t.getComentarios() == null ? 0 : t.getComentarios().size());
        }

        ajustarColunas(sheet, 8);
    }

    private void preencherResumoStatus(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Resumo por status");
        criarCabecalho(sheet, "Status", "Quantidade");

        long aFazer = tarefaRepository.countByStatus("A_FAZER");
        long fazendo = tarefaRepository.countByStatus("FAZENDO");
        long concluido = tarefaRepository.countByStatus("CONCLUIDO");

        int row = 1;
        row = linhaResumo(sheet, row, "A_FAZER", aFazer);
        row = linhaResumo(sheet, row, "FAZENDO", fazendo);
        row = linhaResumo(sheet, row, "CONCLUIDO", concluido);
        linhaResumo(sheet, row, "Total", aFazer + fazendo + concluido);

        ajustarColunas(sheet, 2);
    }

    private int linhaResumo(Sheet sheet, int row, String status, long quantidade) {
        Row r = sheet.createRow(row);
        r.createCell(0).setCellValue(status);
        r.createCell(1).setCellValue(quantidade);
        return row + 1;
    }

    private String nomesResponsaveis(Tarefa tarefa) {
        if (tarefa.getResponsaveis() == null || tarefa.getResponsaveis().isEmpty()) {
            return "";
        }
        return tarefa.getResponsaveis().stream()
                .map(Usuario::getNome)
                .filter(nome -> nome != null && !nome.isBlank())
                .collect(Collectors.joining(", "));
    }

    private void criarCabecalho(Sheet sheet, String... colunas) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            header.createCell(i).setCellValue(colunas[i]);
        }
    }

    private void ajustarColunas(Sheet sheet, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle criarEstiloTexto(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return estilo;
    }

    private void escreverTexto(Row row, int coluna, String valor, CellStyle estiloTexto) {
        Cell cell = row.createCell(coluna, CellType.STRING);
        cell.setCellStyle(estiloTexto);
        cell.setCellValue(sanitizarCelula(valor));
    }

    private String sanitizarCelula(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        int i = 0;
        while (i < valor.length() && Character.isWhitespace(valor.charAt(i))) {
            i++;
        }
        if (i >= valor.length()) {
            return valor;
        }
        // Excel trata =, +, - e @ no começo da célula como fórmula; o apóstrofo evita isso virar código
        char primeiro = valor.charAt(i);
        if (primeiro == '=' || primeiro == '+' || primeiro == '-' || primeiro == '@') {
            return "'" + valor;
        }
        return valor;
    }

    private void garantirAdmin(UsuarioSessao usuario) {
        if (usuario == null || !usuario.isAdmin()) {
            throw new AcessoNegadoException("Apenas administradores podem exportar o relatório.");
        }
    }
}
