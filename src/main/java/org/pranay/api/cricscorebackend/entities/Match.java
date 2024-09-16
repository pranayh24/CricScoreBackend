package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
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
}
