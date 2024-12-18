package org.pranay.api.cricscorebackend.controllers;

import org.pranay.api.cricscorebackend.dto.MatchPredictionDTO;
import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.entities.matchStatus;
import org.pranay.api.cricscorebackend.services.MatchService;
import org.pranay.api.cricscorebackend.services.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prediction")
public class FlaskController {
    private static final Logger logger = LoggerFactory.getLogger(FlaskController.class);
    private final PredictionService predictionService;
    private final MatchService matchService;
    private final ObjectMapper objectMapper;

    @Autowired
    public FlaskController(PredictionService predictionService, MatchService matchService, ObjectMapper objectMapper) {
        this.predictionService = predictionService;
        this.matchService = matchService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<MatchPredictionDTO>> getAllMatchesWithPredictions() {
        try {
            List<Match> matches = matchService.getAllMatches();
            List<MatchPredictionDTO> matchPredictions = matches.stream()
                    .map(match -> {
                        MatchPredictionDTO dto = new MatchPredictionDTO(match);
                        if (match.getStatus() == matchStatus.LIVE) {
                            try {
                                ResponseEntity<String> predictionResponse =
                                        predictionService.getPredictionWithData(
                                                predictionService.extractMatchData(match));
                                if (predictionResponse.getStatusCode() == HttpStatus.OK) {
                                    addPredictionToDTO(dto, predictionResponse.getBody());
                                } else {
                                    dto.setPredictionMessage("Prediction currently unavailable");
                                }
                            } catch (Exception e) {
                                logger.error("Error getting prediction for match {}: {}",
                                        match.getMatchId(), e.getMessage());
                                dto.setPredictionMessage("Error getting prediction");
                            }
                        } else {
                            dto.setPredictionMessage(
                                    match.getStatus() == matchStatus.COMPLETED ?
                                            "Match completed" :
                                            "Prediction not available for this match format");
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(matchPredictions);

        } catch (Exception e) {
            logger.error("Error processing matches and predictions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchPredictionDTO> getMatchPrediction(@PathVariable int matchId) {
        try {
            Match match = matchService.getMatchById(matchId);
            if (match == null) {
                return ResponseEntity.notFound().build();
            }

            MatchPredictionDTO dto = new MatchPredictionDTO(match);

            if (match.getStatus() == matchStatus.LIVE) {
                ResponseEntity<String> predictionResponse =
                        predictionService.getPredictionWithData(
                                predictionService.extractMatchData(match));
                if (predictionResponse.getStatusCode() == HttpStatus.OK) {
                    addPredictionToDTO(dto, predictionResponse.getBody());
                }
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Error getting prediction for match {}: {}", matchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void addPredictionToDTO(MatchPredictionDTO dto, String predictionJson) {
        try {
            var prediction = objectMapper.readTree(predictionJson);
            dto.setIsPredictionAvailable(true);
            dto.setLikelyWinner(prediction.get("likely_winner").asText());
            dto.setWinProbability(prediction.get("win_probability").asDouble());
            dto.setPredictedScore(prediction.get("predicted_final_score").asInt());
        } catch (Exception e) {
            logger.error("Error parsing prediction JSON: {}", e.getMessage());
            dto.setPredictionMessage("Error processing prediction data");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        if (predictionService.isFlaskServiceAvailable()) {
            return ResponseEntity.ok("Prediction service is available");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Prediction service is not available");
        }
    }

    @PostMapping("/updateTossInfo/{matchId}")
    public ResponseEntity<String> updateTossInfo(@PathVariable int matchId, @RequestBody Map<String, String> tossInfo) {
        try {
            Match match = matchService.getMatchById(matchId);
            if (match == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Match not found");
            }

            // Update match data with manual toss info
            match.setTossWinner(tossInfo.get("toss_winner"));
            match.setTossDecision(tossInfo.get("toss_decision"));
            matchService.saveMatch(match);

            return ResponseEntity.ok("Toss information updated successfully");
        } catch (Exception e) {
            logger.error("Error updating toss information: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating toss information");
        }
    }
}