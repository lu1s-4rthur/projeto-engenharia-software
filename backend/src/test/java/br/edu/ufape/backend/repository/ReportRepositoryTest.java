package br.edu.ufape.backend.repository;

import br.edu.ufape.backend.model.Reservation;
import br.edu.ufape.backend.model.Resource;
import br.edu.ufape.backend.model.User;
import br.edu.ufape.backend.model.enums.Role;
import br.edu.ufape.backend.model.enums.StatusReserva;
import br.edu.ufape.backend.model.enums.TipoRecurso;
import br.edu.ufape.backend.repository.projection.ResourceUsageProjection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private static final LocalDate DATA_INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate DATA_FIM = LocalDate.of(2026, 8, 31);

    @BeforeEach
    void setUp() {
        user = User.builder()
                .nome("Usuario Teste")
                .email("usuario.report@teste.com")
                .password("senhaHash")
                .role(Role.USER)
                .build();
        entityManager.persist(user);
    }

    private Resource persistResource(String nome) {
        Resource resource = Resource.builder()
                .nome(nome)
                .descricao("Descricao " + nome)
                .tipo(TipoRecurso.LABORATORIO)
                .statusFuncionamento(true)
                .build();
        entityManager.persist(resource);
        return resource;
    }

    private void persistReservation(Resource resource, LocalDate data, StatusReserva status) {
        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user)
                .data(data)
                .horarioInicio(LocalTime.of(10, 0))
                .horarioFim(LocalTime.of(11, 0))
                .status(status)
                .build();
        entityManager.persist(reservation);
    }

    @Test
    @DisplayName("Reservas CANCELADA nao devem contar no total de reservas do recurso")
    void reservasCanceladas_naoDevemContarNoTotal() {
        Resource resource = persistResource("Laboratorio Report A");

        persistReservation(resource, LocalDate.of(2026, 8, 10), StatusReserva.CONFIRMADA);
        persistReservation(resource, LocalDate.of(2026, 8, 11), StatusReserva.CANCELADA);
        persistReservation(resource, LocalDate.of(2026, 8, 12), StatusReserva.CANCELADA);
        entityManager.flush();

        List<ResourceUsageProjection> resultado = reservationRepository
                .countReservationsByResource(DATA_INICIO, DATA_FIM);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getResourceId()).isEqualTo(resource.getId());
        assertThat(resultado.get(0).getTotalReservas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Reservas RECUSADA tambem nao devem contar no total de reservas do recurso")
    void reservasRecusadas_naoDevemContarNoTotal() {
        Resource resource = persistResource("Laboratorio Report B");

        persistReservation(resource, LocalDate.of(2026, 8, 10), StatusReserva.CONFIRMADA);
        persistReservation(resource, LocalDate.of(2026, 8, 11), StatusReserva.RECUSADA);
        entityManager.flush();

        List<ResourceUsageProjection> resultado = reservationRepository
                .countReservationsByResource(DATA_INICIO, DATA_FIM);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTotalReservas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando so existem reservas canceladas no periodo")
    void deveRetornarVazio_quandoSoExistemReservasCanceladas() {
        Resource resource = persistResource("Laboratorio Report C");

        persistReservation(resource, LocalDate.of(2026, 8, 10), StatusReserva.CANCELADA);
        entityManager.flush();

        List<ResourceUsageProjection> resultado = reservationRepository
                .countReservationsByResource(DATA_INICIO, DATA_FIM);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve identificar corretamente o recurso mais utilizado entre multiplos recursos")
    void deveIdentificarRecursoMaisUtilizado_comMultiplosRecursos() {
        Resource maisUsado = persistResource("Laboratorio Mais Usado");
        Resource menosUsado = persistResource("Laboratorio Menos Usado");

        persistReservation(maisUsado, LocalDate.of(2026, 8, 5), StatusReserva.CONFIRMADA);
        persistReservation(maisUsado, LocalDate.of(2026, 8, 6), StatusReserva.PENDENTE);
        persistReservation(menosUsado, LocalDate.of(2026, 8, 7), StatusReserva.CONFIRMADA);
        entityManager.flush();

        List<ResourceUsageProjection> resultado = reservationRepository
                .countReservationsByResource(DATA_INICIO, DATA_FIM);

        assertThat(resultado).hasSize(2);
        // a query ordena DESC por total, entao o primeiro deve ser o mais utilizado
        assertThat(resultado.get(0).getResourceId()).isEqualTo(maisUsado.getId());
        assertThat(resultado.get(0).getTotalReservas()).isEqualTo(2L);
        assertThat(resultado.get(1).getResourceId()).isEqualTo(menosUsado.getId());
        assertThat(resultado.get(1).getTotalReservas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByDataBetweenAndStatusNotIn deve excluir reservas canceladas e recusadas")
    void findByDataBetweenAndStatusNotIn_deveExcluirCanceladasERecusadas() {
        Resource resource = persistResource("Laboratorio Report D");

        persistReservation(resource, LocalDate.of(2026, 8, 10), StatusReserva.CONFIRMADA);
        persistReservation(resource, LocalDate.of(2026, 8, 11), StatusReserva.CANCELADA);
        persistReservation(resource, LocalDate.of(2026, 8, 12), StatusReserva.RECUSADA);
        entityManager.flush();

        List<StatusReserva> statusIgnorados = List.of(StatusReserva.CANCELADA, StatusReserva.RECUSADA);
        List<Reservation> resultado = reservationRepository
                .findByDataBetweenAndStatusNotIn(DATA_INICIO, DATA_FIM, statusIgnorados);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
    }
}