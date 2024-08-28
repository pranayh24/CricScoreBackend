package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import org.springframework.beans.factory.config.YamlProcessor;

import java.util.Date;

@Entity
@Table(name="cric_matches")
public class Match {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int matchId;
    private String teamHeading;
    private String matchNumberVenue;
    private String battingTeam;
    private String battingTeamScore;
    private String bowlingTeam;
    private String bowlingTeamScore;
    private String liveText;
    private String textComplete;
    private String matchLink;
    @Enumerated
    private matchStatus status;
    private Date date = new Date();

    public Match(int matchId, String teamHeading, String matchNumberVenue, String battingTeam, String battingTeamScore, String bowlingTeam, String bowlingTeamScore, String liveText, String textComplete, String matchLink, matchStatus status, Date date) {
        this.matchId = matchId;
        this.teamHeading = teamHeading;
        this.matchNumberVenue = matchNumberVenue;
        this.battingTeam = battingTeam;
        this.battingTeamScore = battingTeamScore;
        this.bowlingTeam = bowlingTeam;
        this.bowlingTeamScore = bowlingTeamScore;
        this.liveText = liveText;
        this.textComplete = textComplete;
        this.matchLink = matchLink;
        this.status = status;
        this.date = date;
    }

    public Match(){
    }
    // set the match status according to text complete
    public void setMatchStatus(){
        if(textComplete.isBlank()){
            this.status= matchStatus.LIVE;
        }
        else{
            this.status = matchStatus.COMPLETED;
        }
    }
    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public String getTeamHeading() {
        return teamHeading;
    }

    public void setTeamHeading(String teamHeading) {
        this.teamHeading = teamHeading;
    }

    public String getMatchNumberVenue() {
        return matchNumberVenue;
    }

    public void setMatchNumberVenue(String matchNumberVenue) {
        this.matchNumberVenue = matchNumberVenue;
    }

    public String getBattingTeam() {
        return battingTeam;
    }

    public void setBattingTeam(String battingTeam) {
        this.battingTeam = battingTeam;
    }

    public String getBattingTeamScore() {
        return battingTeamScore;
    }

    public void setBattingTeamScore(String battingTeamScore) {
        this.battingTeamScore = battingTeamScore;
    }

    public String getBowlingTeam() {
        return bowlingTeam;
    }

    public void setBowlingTeam(String bowlingTeam) {
        this.bowlingTeam = bowlingTeam;
    }

    public String getBowlingTeamScore() {
        return bowlingTeamScore;
    }

    public void setBowlingTeamScore(String bowlingTeamScore) {
        this.bowlingTeamScore = bowlingTeamScore;
    }

    public String getLiveText() {
        return liveText;
    }

    public void setLiveText(String liveText) {
        this.liveText = liveText;
    }

    public String getTextComplete() {
        return textComplete;
    }

    public void setTextComplete(String textComplete) {
        this.textComplete = textComplete;
    }

    public String getMatchLink() {
        return matchLink;
    }

    public void setMatchLink(String matchLink) {
        this.matchLink = matchLink;
    }

    public matchStatus getStatus() {
        return status;
    }

    public void setStatus(matchStatus status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
