package com.careertrack.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Informations de l'utilisateur connecté")
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private boolean enabled;
    private List<String> roles;
    private UUID employeeId;
}
