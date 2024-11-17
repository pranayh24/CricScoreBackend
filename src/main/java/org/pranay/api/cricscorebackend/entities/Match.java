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
    private String matchFormat;

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
