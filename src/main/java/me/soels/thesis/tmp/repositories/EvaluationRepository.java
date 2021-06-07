package me.soels.thesis.tmp.repositories;

import me.soels.thesis.tmp.daos.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
}
