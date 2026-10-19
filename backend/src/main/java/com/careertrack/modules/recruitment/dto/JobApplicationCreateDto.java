package com.careertrack.modules.recruitment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationCreateDto {
    @NotNull(message = "L'offre d'emploi est obligatoire")
    private UUID jobOfferId;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String phone;
    private String cvUrl;
    private String linkedinUrl;
    private String notes;
}
