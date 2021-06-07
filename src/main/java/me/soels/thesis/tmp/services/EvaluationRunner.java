package me.soels.thesis.tmp.services;

import me.soels.thesis.tmp.daos.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EvaluationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);

    @Async
    public void runEvaluation(Evaluation evaluation) {
        LOGGER.info("Running evaluation '{}' ({})", evaluation.getName(), evaluation.getId());
        // TODO: Retrieve graph and other inputs (in separate service?)
        // TODO: Run clustering using MOEA framework
        // TODO: Set status on error or success.
        // TODO: Set metric information (runtime etc.).
        // TODO: Store results (in separate service?)
    }
}
