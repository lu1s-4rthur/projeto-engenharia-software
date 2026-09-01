package br.edu.ufape.backend.service;

import br.edu.ufape.backend.dto.UsageReportResponse;
import br.edu.ufape.backend.model.Reservation;
import br.edu.ufape.backend.model.enums.StatusReserva;
import br.edu.ufape.backend.repository.ReservationRepository;
import br.edu.ufape.backend.repository.projection.ResourceUsageProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReportService reportService;

    private static final LocalDate DATA_INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate DATA_FIM = LocalDate.of(2026, 8, 31);
    private static final List<StatusReserva> STATUS_IGNORADOS =
            List.of(StatusReserva.CANCELADA, StatusReserva.RECUSADA);

    @Test
    @DisplayName("Deve retornar indicadores zerados quando nao ha reservas no periodo")
    void deveRetornarIndicadoresZerados_quandoPeriodoSemReservas() {
        when(reservationRepository.countReservationsByResource(DATA_INICIO, DATA_FIM))
                .thenReturn(List.of());
        when(reservationRepository.findByDataBetweenAndStatusNotIn(DATA_INICIO, DATA_FIM, STATUS_IGNORADOS))
                .thenReturn(List.of());

        UsageReportResponse response = reportService.gerarRelatorioUtilizacao(DATA_INICIO, DATA_FIM);

        assertThat(response.getReservasPorRecurso()).isEmpty();
        assertThat(response.getRecursoMaisUtilizado()).isNull();
        assertThat(response.getRecursoMenosUtilizado()).isNull();
        assertThat(response.getReservasPorDiaSemana()).isEmpty();
    }

    @Test
    @DisplayName("Deve identificar corretamente o recurso mais e o menos utilizado com multiplos recursos")
    void deveIdentificarMaisEMenosUtilizado_comMultiplosRecursos() {
        ResourceUsageProjection maisUsado = mock(ResourceUsageProjection.class);
        when(maisUsado.getResourceId()).thenReturn(1L);
        when(maisUsado.getResourceNome()).thenReturn("Laboratorio A");
        when(maisUsado.getTotalReservas()).thenReturn(10L);

        ResourceUsageProjection intermediario = mock(ResourceUsageProjection.class);
        when(intermediario.getResourceId()).thenReturn(2L);
        when(intermediario.getResourceNome()).thenReturn("Laboratorio B");
        when(intermediario.getTotalReservas()).thenReturn(5L);

        ResourceUsageProjection menosUsado = mock(ResourceUsageProjection.class);
        when(menosUsado.getResourceId()).thenReturn(3L);
        when(menosUsado.getResourceNome()).thenReturn("Laboratorio C");
        when(menosUsado.getTotalReservas()).thenReturn(1L);

        // a query real ja retorna ordenado DESC por total, entao o mock reflete isso
        when(reservationRepository.countReservationsByResource(DATA_INICIO, DATA_FIM))
                .thenReturn(List.of(maisUsado, intermediario, menosUsado));
        when(reservationRepository.findByDataBetweenAndStatusNotIn(DATA_INICIO, DATA_FIM, STATUS_IGNORADOS))
                .thenReturn(List.<Reservation>of());

        UsageReportResponse response = reportService.gerarRelatorioUtilizacao(DATA_INICIO, DATA_FIM);

        assertThat(response.getReservasPorRecurso()).hasSize(3);
        assertThat(response.getRecursoMaisUtilizado().getResourceId()).isEqualTo(1L);
        assertThat(response.getRecursoMaisUtilizado().getTotalReservas()).isEqualTo(10L);
        assertThat(response.getRecursoMenosUtilizado().getResourceId()).isEqualTo(3L);
        assertThat(response.getRecursoMenosUtilizado().getTotalReservas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lancar 400 quando a data de fim for anterior a data de inicio")
    void deveLancar400_quandoDataFimAnteriorADataInicio() {
        LocalDate dataInicio = LocalDate.of(2026, 8, 10);
        LocalDate dataFim = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> reportService.gerarRelatorioUtilizacao(dataInicio, dataFim))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}