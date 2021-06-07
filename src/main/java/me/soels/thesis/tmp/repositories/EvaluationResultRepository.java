package me.soels.thesis.tmp.repositories;

import me.soels.thesis.tmp.daos.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, UUID> {
}
