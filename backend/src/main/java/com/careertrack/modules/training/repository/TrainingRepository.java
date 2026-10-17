package com.careertrack.modules.training.repository;

import com.careertrack.modules.training.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingRepository extends JpaRepository<Training, UUID> {
    Optional<Training> findByCode(String code);
}
