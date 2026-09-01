package br.edu.ufape.backend.controller;

import br.edu.ufape.backend.dto.UsageReportResponse;
import br.edu.ufape.backend.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/utilizacao")
    public ResponseEntity<UsageReportResponse> consultarRelatorioUtilizacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        UsageReportResponse response = reportService.gerarRelatorioUtilizacao(dataInicio, dataFim);
        return ResponseEntity.ok(response);
    }
}