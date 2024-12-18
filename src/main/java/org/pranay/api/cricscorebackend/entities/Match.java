package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
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
    private String matchType;
    private Date date = new Date();

    private String tossWinner;
    private String tossDecision;

    // Getters and setters
    public String getTossWinner() {
        return tossWinner;
    }

    public void setTossWinner(String tossWinner) {
        this.tossWinner = tossWinner;
    }

    public String getTossDecision() {
        return tossDecision;
    }

    public void setTossDecision(String tossDecision) {
        this.tossDecision = tossDecision;
    }
    // set the match status according to text complete
    @Column(name = "match_format")
    private String matchFormat;
    // Getter and Setter for matchFormat
    public String getMatchFormat() {
        return matchFormat;
    }

    public void setMatchFormat(String matchFormat) {
        this.matchFormat = matchFormat;
    }

    public void setMatchStatus(){
        if(textComplete.isBlank()){
            this.status= matchStatus.LIVE;
        }
        else{
            this.status = matchStatus.COMPLETED;
        }
    }
}
