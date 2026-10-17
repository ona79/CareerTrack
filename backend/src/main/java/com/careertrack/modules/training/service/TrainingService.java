package com.careertrack.modules.training.service;

import com.careertrack.modules.training.dto.TrainingDto;

import java.util.List;
import java.util.UUID;

public interface TrainingService {
    List<TrainingDto> getAllTrainings();
    TrainingDto getTrainingById(UUID id);
    TrainingDto createTraining(TrainingDto dto);
}
