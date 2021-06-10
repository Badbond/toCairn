package me.soels.thesis.repositories;

import me.soels.thesis.model.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {
}
