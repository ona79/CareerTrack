package com.careertrack.modules.training.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.training.dto.TrainingDto;
import com.careertrack.modules.training.entity.Training;
import com.careertrack.modules.training.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    @Mock
    private TrainingRepository trainingRepository;

    private TrainingServiceImpl trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingServiceImpl(trainingRepository);
    }

    @Test
    @DisplayName("Lister toutes les formations du catalogue")
    void shouldGetAllTrainings() {
        Training t = Training.builder()
                .id(UUID.randomUUID())
                .code("DEV-01")
                .title("Spring Boot & Microservices")
                .durationHours(35)
                .category("Technique")
                .provider("Dakar Tech Academy")
                .build();

        when(trainingRepository.findAll()).thenReturn(List.of(t));

        List<TrainingDto> result = trainingService.getAllTrainings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("DEV-01");
        assertThat(result.get(0).getTitle()).isEqualTo("Spring Boot & Microservices");
    }

    @Test
    @DisplayName("Obtenir une formation par ID ou lever ResourceNotFoundException")
    void shouldGetTrainingById() {
        UUID id = UUID.randomUUID();
        Training t = Training.builder().id(id).title("Management Agile").build();

        when(trainingRepository.findById(id)).thenReturn(Optional.of(t));

        TrainingDto result = trainingService.getTrainingById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("Management Agile");

        when(trainingRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainingService.getTrainingById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Créer une nouvelle session de formation au catalogue")
    void shouldCreateTraining() {
        TrainingDto dto = TrainingDto.builder()
                .code("RH-01")
                .title("Droit du Travail Sénégalais")
                .durationHours(20)
                .category("Réglementaire")
                .provider("Cabinet Juridique Dakar")
                .build();

        Training saved = Training.builder()
                .id(UUID.randomUUID())
                .code(dto.getCode())
                .title(dto.getTitle())
                .durationHours(dto.getDurationHours())
                .category(dto.getCategory())
                .provider(dto.getProvider())
                .build();

        when(trainingRepository.save(any(Training.class))).thenReturn(saved);

        TrainingDto result = trainingService.createTraining(dto);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getCode()).isEqualTo("RH-01");
    }
}
