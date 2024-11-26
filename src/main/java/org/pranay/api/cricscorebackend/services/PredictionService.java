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
        //initializeAdditionalStadiumMappings();
    }

    /*private void initializeAdditionalStadiumMappings() {
        StadiumCityMapper.addStadiumMapping("Pallekele International Cricket Stadium", "Kandy");
        StadiumCityMapper.addStadiumMapping("R.Premadasa Stadium", "Colombo");
        StadiumCityMapper.addStadiumMapping("Premadasa Stadium", "Colombo");
        StadiumCityMapper.addStadiumMapping("Melbourne Cricket Ground", "Melbourne");
        StadiumCityMapper.addStadiumMapping("Sydney Cricket Ground", "Sydney");
        StadiumCityMapper.addStadiumMapping("Adelaide Oval", "Adelaide");
        StadiumCityMapper.addStadiumMapping("WACA Ground", "Perth");
        StadiumCityMapper.addStadiumMapping("Bellerive Oval", "Hobart");
    }
*/
    public Map<String, Object> extractMatchData(Match match) {
        try {
            Map<String, Object> data = new HashMap<>();

            // Extract and validate teams
            String[] teams = extractAndValidateTeams(match.getTeamHeading());
            if (teams == null) return null;

            String team1 = TeamNameStandardizer.standardizeTeamName(teams[0]);
            String team2 = TeamNameStandardizer.standardizeTeamName(teams[1]);
            data.put("team1", team1);
            data.put("team2", team2);

            // Extract and validate city
            String city = extractAndValidateCity(match.getMatchNumberVenue());
            if (city == null) return null;
            data.put("city", city);

            // Parse score details
            ScoreDetails battingDetails = parseScore(match.getBattingTeamScore());
            if (battingDetails == null) {
                logger.warn("Could not parse batting score: {}", match.getBattingTeamScore());
                return null;
            }

            data.put("current_score", battingDetails.runs);
            data.put("current_wickets", battingDetails.wickets);
            data.put("current_over", battingDetails.overs);

            // Extract and validate toss details
            Map<String, String> tossDetails = extractTossDetails(match);
            if (tossDetails == null) return null;

            String standardizedBattingTeam = TeamNameStandardizer.standardizeTeamName(match.getBattingTeam());

            // Validate batting team
            if (!standardizedBattingTeam.equals(team1) && !standardizedBattingTeam.equals(team2)) {
                logger.error("Batting team '{}' does not match either team1 '{}' or team2 '{}'",
                        standardizedBattingTeam, team1, team2);
                return null;
            }

            data.put("batting_team", standardizedBattingTeam);
            data.put("toss_winner", TeamNameStandardizer.standardizeTeamName(tossDetails.get("winner")));
            data.put("toss_decision", tossDetails.get("decision"));

            // Extract and validate match format
            String format = extractAndValidateFormat(match);
            if (format == null) return null;
            data.put("match_format", format);

            logger.debug("Extracted match data: {}", data);
            return data;

        } catch (Exception e) {
            logger.error("Error extracting match data: {}", e.getMessage(), e);
            return null;
        }
    }

    private String[] extractAndValidateTeams(String teamHeading) {
        if (teamHeading == null) {
            logger.warn("Team heading is null");
            return null;
        }

        String cleanedHeading = teamHeading.replaceAll(",$", "");
        String[] teams = cleanedHeading.split("vs|VS");

        if (teams.length != 2) {
            logger.warn("Invalid team heading format: {}", teamHeading);
            return null;
        }

        return new String[]{teams[0].trim(), teams[1].trim()};
    }

    private String extractAndValidateCity(String venue) {
        if (venue == null) {
            logger.warn("Venue is null");
            return null;
        }

        String city = StadiumCityMapper.getCityFromVenue(venue);
        if (city.isEmpty()) {
            logger.debug("Could not map venue to city: {}. Falling back to direct extraction.", venue);
            city = extractCityFallback(venue);
        }

        if (city.isEmpty()) {
            logger.warn("Could not extract city from match venue: {}", venue);
            return null;
        }

        return city;
    }

    private String extractCityFallback(String matchNumberVenue) {
        if (matchNumberVenue == null) return "";

        String[] parts = matchNumberVenue.split("•");
        if (parts.length > 1) {
            String venue = parts[1].trim()
                    .replaceAll("(?i)^\\s*at\\s+", "")
                    .split(",")[0]
                    .split("\\s*\\(")[0]
                    .trim();

            venue = venue.replaceAll("(?i)\\s*Stadium.*$", "")
                    .replaceAll("(?i)\\s*Ground.*$", "")
                    .replaceAll("(?i)\\s*Oval.*$", "")
                    .trim();

            return venue;
        }
        return "";
    }

    private Map<String, String> extractTossDetails(Match match) {
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

        if (tossWinner.isEmpty() || tossDecision.isEmpty()) {
            tossWinner = getManualTossWinner(match);
            tossDecision = getManualTossDecision(match);
        }

        if (tossWinner.isEmpty() || tossDecision.isEmpty()) {
            logger.warn("Could not determine toss details");
            return null;
        }

        Map<String, String> details = new HashMap<>();
        details.put("winner", tossWinner);
        details.put("decision", tossDecision);
        return details;
    }

    private String extractAndValidateFormat(Match match) {
        String format = match.getMatchFormat();
        if (format == null || format.equals("UNKNOWN")) {
            if (match.getTeamHeading().toLowerCase().contains("women")) {
                format = "T20";
            } else {
                logger.warn("Could not determine match format");
                return null;
            }
        }
        format = format.toUpperCase();
        if (!isValidFormat(format)) {
            logger.warn("Invalid match format: {}", format);
            return null;
        }
        return format;
    }

    private ScoreDetails parseScore(String score) {
        if (score == null || score.trim().isEmpty()) {
            return null;
        }

        try {
            Matcher matcher = SCORE_PATTERN.matcher(score);
            if (matcher.find()) {
                return new ScoreDetails(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Double.parseDouble(matcher.group(3))
                );
            }
        } catch (Exception e) {
            logger.error("Error parsing score {}: {}", score, e.getMessage());
        }
        return null;
    }

    public ResponseEntity<String> getPredictionWithData(Map<String, Object> matchData) {
        try {
            if (matchData == null || matchData.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("Match data is empty or null");
            }

            if (!isFlaskServiceAvailable()) {
                logger.error("Flask prediction service is not available");
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Prediction service is currently unavailable");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(matchData, headers);

            logger.info("Sending prediction request for {} match", matchData.get("match_format"));
            logger.debug("Request data: {}", matchData);

            ResponseEntity<String> response = restTemplate.exchange(
                    flaskBaseUrl + "/predict",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            logger.info("Received prediction response");
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

    private String getManualTossWinner(Match match) {
        return match.getBattingTeam();
    }

    private String getManualTossDecision(Match match) {
        return "bat";
    }
}