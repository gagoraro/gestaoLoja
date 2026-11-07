package br.com.gestaoloja;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com login ligado: sem token = 401; com token válido = acesso liberado. */
@SpringBootTest(properties = {"spring.profiles.active=dev", "app.auth.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:seguranca;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class SegurancaTest {

    @Autowired MockMvc mvc;

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/caixa/atual")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/produtos").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/produtos").with(jwt().jwt(j -> j.claim("email", "loja@teste.com"))))
                .andExpect(status().isOk());
    }
}
