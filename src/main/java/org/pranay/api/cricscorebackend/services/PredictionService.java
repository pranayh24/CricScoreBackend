package org.pranay.api.cricscorebackend.services;

import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.entities.ScoreDetails;
import org.pranay.api.cricscorebackend.helper.StadiumCityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PredictionService {
    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);
    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d+)-(\\d+)\\s*\\((\\d+(?:\\.\\d+)?)\\s*Ovs\\)");

    @Autowired
    public PredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.flaskBaseUrl = "http://localhost:5000";

        // Initialize with some common stadium mappings that might be missing
        initializeAdditionalStadiumMappings();
    }

    private void initializeAdditionalStadiumMappings() {
        // Add any additional mappings that might be specific to your data
        StadiumCityMapper.addStadiumMapping("Pallekele International Cricket Stadium", "Kandy");
        StadiumCityMapper.addStadiumMapping("R.Premadasa Stadium", "Colombo");
        StadiumCityMapper.addStadiumMapping("Premadasa Stadium", "Colombo");
        // Add more mappings as needed
    }

    public Map<String, Object> extractMatchData(Match match) {
        try {
            Map<String, Object> data = new HashMap<>();

            // Extract teams from team heading (remove trailing comma if present)
            String teamHeading = match.getTeamHeading().replaceAll(",$", "");
            String[] teams = teamHeading.split("vs|VS");
            if (teams.length != 2) {
                logger.warn("Invalid team heading format: {}", match.getTeamHeading());
                return null;
            }

            String team1 = teams[0].trim();
            String team2 = teams[1].trim();
            data.put("team1", team1);
            data.put("team2", team2);

            // Extract city from venue using StadiumCityMapper
            String venue = match.getMatchNumberVenue();
            String city = StadiumCityMapper.getCityFromVenue(venue);

            if (city.isEmpty()) {
                logger.warn("Could not map venue to city: {}. Falling back to direct extraction.", venue);
                // Fallback to original extraction method
                city = extractCityFallback(venue);
            }

            if (city.isEmpty()) {
                logger.warn("Could not extract city from match venue: {}", venue);
                return null;
            }

            // Log the mapped city for debugging
            logger.debug("Mapped venue '{}' to city '{}'", venue, city);
            data.put("city", city);

            // Rest of your existing code remains the same
            ScoreDetails battingDetails = parseScore(match.getBattingTeamScore());
            if (battingDetails != null) {
                data.put("current_score", battingDetails.runs);
                data.put("current_wickets", battingDetails.wickets);
                data.put("current_over", battingDetails.overs);
            } else {
                logger.warn("Could not parse batting score: {}", match.getBattingTeamScore());
                return null;
            }

            String liveText = match.getLiveText().toLowerCase();
            String tossWinner = "";
            String tossDecision = "";

            if (liveText.contains("opt to") || liveText.contains("opted to")) {
                String[] parts = liveText.split("opt");
                if (parts.length > 0) {
                    tossWinner = parts[0].trim();
                    tossDecision = liveText.contains("bowl") ? "field" : "bat";
                }
            }

            data.put("batting_team", match.getBattingTeam());
            data.put("toss_winner", tossWinner);
            data.put("toss_decision", tossDecision);

            String format = match.getMatchFormat();
            if (format == null || format.equals("UNKNOWN")) {
                if (teamHeading.toLowerCase().contains("women")) {
                    format = "T20";
                }
            }
            data.put("match_format", format != null ? format.toUpperCase() : "UNKNOWN");

            logger.debug("Extracted match data: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error extracting match data: {}", e.getMessage(), e);
            return null;
        }
    }

    // Fallback method for city extraction if mapping fails
    private String extractCityFallback(String matchNumberVenue) {
        if (matchNumberVenue == null) return "";

        String[] parts = matchNumberVenue.split("•");
        if (parts.length > 1) {
            String venue = parts[1].trim();
            venue = venue.replaceAll("(?i)^\\s*at\\s+", "")
                    .split(",")[0]
                    .split("\\s*\\(")[0]
                    .trim();

            if (venue.contains("Stadium")) {
                venue = venue.split("Stadium")[0].trim();
            }
            if (venue.contains("Ground")) {
                venue = venue.split("Ground")[0].trim();
            }
            if (venue.contains("Oval")) {
                venue = venue.split("Oval")[0].trim();
            }

            return venue;
        }
        return "";
    }

    private ScoreDetails parseScore(String score) {
        if (score == null || score.trim().isEmpty()) {
            return null;
        }

        try {
            Matcher matcher = SCORE_PATTERN.matcher(score);
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