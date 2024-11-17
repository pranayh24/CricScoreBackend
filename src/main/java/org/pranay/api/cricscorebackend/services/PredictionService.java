package org.pranay.api.cricscorebackend.services;

import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.entities.ScoreDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {
    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);
    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    @Autowired
    public PredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.flaskBaseUrl = "http://localhost:5000";
    }

    public Map<String, Object> extractMatchData(Match match) {
        try {
            Map<String, Object> data = new HashMap<>();

            // Extract teams from team heading
            String[] teams = match.getTeamHeading().split("vs|VS");
            if (teams.length != 2) {
                logger.warn("Invalid team heading format: {}", match.getTeamHeading());
                return null;
            }

            data.put("team1", teams[0].trim());
            data.put("team2", teams[1].trim());

            // Extract city from venue
            String city = extractCity(match.getMatchNumberVenue());
            data.put("city", city);

            // Extract current score and wickets
            ScoreDetails battingDetails = parseScore(match.getBattingTeamScore());
            if (battingDetails != null) {
                data.put("current_score", battingDetails.runs);
                data.put("current_wickets", battingDetails.wickets);
                data.put("current_over", battingDetails.overs);
            } else {
                return null;
            }

            // Determine if it's first or second innings
            boolean isSecondInnings = match.getBowlingTeamScore() != null &&
                    !match.getBowlingTeamScore().trim().isEmpty();

            if (isSecondInnings) {
                ScoreDetails bowlingDetails = parseScore(match.getBowlingTeamScore());
                if (bowlingDetails != null) {
                    data.put("target", bowlingDetails.runs + 1);
                    data.put("batting_first", 0);
                } else {
                    return null;
                }
            } else {
                data.put("batting_first", 1);
            }

            data.put("batting_team", match.getBattingTeam());
            data.put("match_format", match.getMatchFormat().toUpperCase());
            data.put("toss_winner", match.getBattingTeam());
            data.put("toss_decision", isSecondInnings ? "field" : "bat");

            return data;

        } catch (Exception e) {
            logger.error("Error extracting match data: {}", e.getMessage());
            return null;
        }
    }

    private String extractCity(String matchNumberVenue) {
        if (matchNumberVenue != null && matchNumberVenue.contains(",")) {
            return matchNumberVenue.split(",")[1].trim();
        }
        return "";
    }

    private ScoreDetails parseScore(String score) {
        if (score != null && score.contains("/")) {
            String[] parts = score.split("/");
            int runs = Integer.parseInt(parts[0].trim());
            String[] wicketsAndOvers = parts[1].split("\\(");
            int wickets = Integer.parseInt(wicketsAndOvers[0].trim());
            double overs = Double.parseDouble(wicketsAndOvers[1].replace(")", "").trim());
            return new ScoreDetails(runs, wickets, overs);
        }
        return null;
    }

    public ResponseEntity<String> getPredictionWithData(Map<String, Object> matchData) {
        try {
            // Validate the match format
            String format = (String) matchData.getOrDefault("match_format", "");
            if (!isValidFormat(format)) {
                return ResponseEntity
                        .badRequest()
                        .body("Invalid match format: " + format);
            }

            // Check if Flask service is available
            if (!isFlaskServiceAvailable()) {
                logger.error("Flask prediction service is not available");
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Prediction service is currently unavailable");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(matchData, headers);

            logger.info("Sending prediction request for {} match", format);
            logger.debug("Request data: {}", matchData);

            ResponseEntity<String> response = restTemplate.exchange(
                    flaskBaseUrl + "/predict",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            logger.info("Received prediction response for {} match", format);
            logger.debug("Response: {}", response.getBody());

            return response;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during prediction request: {}", e.getMessage());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body("Error from prediction service: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error getting prediction: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting prediction: " + e.getMessage());
        }
    }

    private boolean isValidFormat(String format) {
        if (format == null) return false;
        String upperFormat = format.toUpperCase();
        return upperFormat.equals("ODI") || upperFormat.equals("T20");
    }

    public boolean isFlaskServiceAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    flaskBaseUrl + "/health",
                    String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Error checking Flask service availability: {}", e.getMessage());
            return false;
        }
    }
}