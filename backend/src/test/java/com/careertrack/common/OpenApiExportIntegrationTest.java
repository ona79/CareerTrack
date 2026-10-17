package com.careertrack.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Génère docs/api/openapi.yaml directement depuis /v3/api-docs.yaml de Springdoc")
    void exportOpenApiSpecification() throws Exception {
        byte[] response = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(response).isNotEmpty();

        Path target = Paths.get("../docs/api/openapi.yaml").toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.write(target, response);

        assertThat(Files.exists(target)).isTrue();
        System.out.println(">>> docs/api/openapi.yaml généré avec succès depuis /v3/api-docs.yaml (taille: " + response.length + " octets)");
    }
}
