package com.careertrack.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${careertrack.company-name:Sénégal Digital Solutions PME}")
    private String companyName;

    @Bean
    public OpenAPI careerTrackOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("CareerTrack API - Plateforme RH")
                        .description("API REST de gestion des carrières pour PME sénégalaise (" + companyName + ")\n" +
                                "- Authentification JWT multi-rôles (DRH, MANAGER, EMPLOYE, DIRECTION)\n" +
                                "- Conforme au Code du Travail sénégalais et calculs en Francs CFA (XOF)\n" +
                                "- Gestion complète du cycle RH : dossier, congés, évaluations, formation, paie, recrutement")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe Projet CareerTrack")
                                .email("contact@careertrack.sn"))
                        .license(new License()
                                .name("Projet Universitaire - Licence Académique")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Environnement Local Développement"),
                        new Server().url("/").description("Serveur Actuel")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insérer le token JWT sous la forme : `Bearer <votre_token>`")));
    }
}
