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
        // Only process if match is live and format is ODI or T20
        if (match.getStatus() == matchStatus.LIVE && isValidFormat(match.getMatchFormat())) {
            logger.info("Processing prediction for {} match: {}", match.getMatchFormat(), match.getTeamHeading());
            Map<String, Object> matchData = extractMatchData(match);
            if (matchData != null) {
                predictionService.getPredictionWithData(matchData);
            } else {
                logger.warn("Could not extract match data for prediction: {}", match.getTeamHeading());
            }
        } else {
            logger.debug("Skipping prediction for match: {} (Format: {}, Status: {})",
                    match.getTeamHeading(), match.getMatchFormat(), match.getStatus());
        }
    }

    private boolean isValidFormat(String format) {
        if (format == null) return false;
        String upperFormat = format.toUpperCase();
        return upperFormat.equals("ODI") || upperFormat.equals("T20");
    }

    private Map<String, Object> extractMatchData(Match match) {
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
                logger.warn("Could not parse batting score: {}", match.getBattingTeamScore());
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
                    logger.warn("Could not parse bowling score: {}", match.getBowlingTeamScore());
                    return null;
                }
            } else {
                data.put("batting_first", 1);
            }

            data.put("batting_team", match.getBattingTeam());
            // Ensure match format is correctly formatted for the Flask API
            data.put("match_format", match.getMatchFormat().toUpperCase());

            // Handle toss details (you might need to add these to your Match entity)
            // For now, using placeholder logic
            data.put("toss_winner", match.getBattingTeam());
            data.put("toss_decision", isSecondInnings ? "field" : "bat");

            logger.debug("Extracted match data: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error extracting match data: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractCity(String matchNumberVenue) {
        // Extract city from venue string (e.g., "3rd ODI • Mumbai")
        Pattern pattern = Pattern.compile("•\\s*([^•]+)$");
        Matcher matcher = pattern.matcher(matchNumberVenue);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Unknown";
    }

    private static class ScoreDetails {
        int runs;
        int wickets;
        double overs;

        ScoreDetails(int runs, int wickets, double overs) {
            this.runs = runs;
            this.wickets = wickets;
            this.overs = overs;
        }

        @Override
        public String toString() {
            return String.format("%d/%d (%s)", runs, wickets, overs);
        }
    }

    private ScoreDetails parseScore(String score) {
        try {
            // Parse score like "240/3 (43.2)"
            Pattern pattern = Pattern.compile("(\\d+)/(\\d+)\\s*\\((\\d+\\.?\\d*)\\)");
            Matcher matcher = pattern.matcher(score);

            if (matcher.find()) {
                int runs = Integer.parseInt(matcher.group(1));
                int wickets = Integer.parseInt(matcher.group(2));
                double overs = Double.parseDouble(matcher.group(3));
                return new ScoreDetails(runs, wickets, overs);
            }
        } catch (Exception e) {
            logger.error("Error parsing score {}: {}", score, e.getMessage());
        }
        return null;
    }
}