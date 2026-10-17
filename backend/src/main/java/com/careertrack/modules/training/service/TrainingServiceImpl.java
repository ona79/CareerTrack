package com.careertrack.modules.training.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.training.dto.TrainingDto;
import com.careertrack.modules.training.entity.Training;
import com.careertrack.modules.training.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRepository trainingRepository;

    public TrainingServiceImpl(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Override
    public List<TrainingDto> getAllTrainings() {
        return trainingRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public TrainingDto getTrainingById(UUID id) {
        Training t = trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable : " + id));
        return mapToDto(t);
    }

    @Override
    @Transactional
    public TrainingDto createTraining(TrainingDto dto) {
        Training t = Training.builder()
                .code(dto.getCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .durationHours(dto.getDurationHours())
                .provider(dto.getProvider())
                .category(dto.getCategory())
                .build();
        return mapToDto(trainingRepository.save(t));
    }

    private TrainingDto mapToDto(Training t) {
        return TrainingDto.builder()
                .id(t.getId())
                .code(t.getCode())
                .title(t.getTitle())
                .description(t.getDescription())
                .durationHours(t.getDurationHours())
                .provider(t.getProvider())
                .category(t.getCategory())
                .build();
    }
}
