package br.edu.ufape.backend.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ReportSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private static final String URL_RELATORIO =
            "/api/reports/utilizacao?dataInicio=2026-08-01&dataFim=2026-08-31";

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("USER comum nao pode acessar o relatorio de utilizacao - 403 Forbidden")
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getRelatorio_comRoleUser_deveRetornar403() throws Exception {
        mockMvc.perform(get(URL_RELATORIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem autenticacao nao pode acessar o relatorio de utilizacao - 401/403")
    void getRelatorio_semAutenticacao_deveRetornarNaoAutorizado() throws Exception {
        mockMvc.perform(get(URL_RELATORIO))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("ADMIN pode acessar o relatorio de utilizacao - passa na seguranca")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getRelatorio_comRoleAdmin_devePassarSeguranca() throws Exception {
        mockMvc.perform(get(URL_RELATORIO))
                .andExpect(status().isOk());
    }
}