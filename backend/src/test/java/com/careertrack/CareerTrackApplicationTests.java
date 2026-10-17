package com.careertrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CareerTrackApplicationTests {

    @Test
    void contextLoads() {
        // Vérifie le chargement correct du contexte Spring Boot et des entités JPA
    }
}
