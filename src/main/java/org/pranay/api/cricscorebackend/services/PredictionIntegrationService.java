package org.pranay.api.cricscorebackend.services;

import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.entities.matchStatus;
import org.pranay.api.cricscorebackend.services.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PredictionIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(PredictionIntegrationService.class);
    private final PredictionService predictionService;

    public PredictionIntegrationService(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    public void processPredictionForMatch(Match match) {
        if (match.getStatus() == matchStatus.LIVE) {
            logger.info("Processing prediction for match: {}", match.getTeamHeading());
            Map<String, Object> matchData = predictionService.extractMatchData(match);
            if (matchData != null) {
                predictionService.getPredictionWithData(matchData);
            } else {
                logger.warn("Could not extract match data for prediction: {}", match.getTeamHeading());
            }
        } else {
            logger.debug("Skipping prediction for match: {} (Status: {})",
                    match.getTeamHeading(), match.getStatus());
        }
    }
}