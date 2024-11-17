package org.pranay.api.cricscorebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.pranay.api.cricscorebackend.entities.Match;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchPredictionDTO {
    // Match details
    private int matchId;
    private String teamHeading;
    private String matchNumberVenue;
    private String battingTeam;
    private String battingTeamScore;
    private String bowlingTeam;
    private String bowlingTeamScore;
    private String liveText;
    private String matchFormat;
    private String matchStatus;
    private Date lastUpdated;

    // Prediction details
    private String likelyWinner;
    private Double winProbability;
    private Integer predictedScore;
    private String predictionMessage;
    private Boolean isPredictionAvailable;

    // Constructor from Match entity
    public MatchPredictionDTO(Match match) {
        this.matchId = match.getMatchId();
        this.teamHeading = match.getTeamHeading();
        this.matchNumberVenue = match.getMatchNumberVenue();
        this.battingTeam = match.getBattingTeam();
        this.battingTeamScore = match.getBattingTeamScore();
        this.bowlingTeam = match.getBowlingTeam();
        this.bowlingTeamScore = match.getBowlingTeamScore();
        this.liveText = match.getLiveText();
        this.matchFormat = match.getMatchFormat();
        this.matchStatus = match.getStatus().toString();
        this.lastUpdated = match.getDate();
        this.isPredictionAvailable = false; // Default value
    }
}