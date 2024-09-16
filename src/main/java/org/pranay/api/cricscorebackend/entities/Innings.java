package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="innings")
public class Innings {

    @Id
    private int inningsId;

    @ManyToOne
    @JoinColumn(name="scorecard_id")
    private Scorecard scorecard;

    private String teamName;
    private int runsScored;
    private int wicketsLost;
    private String overBowled;
    private int extras;
    private String fallOfWickets;

    private List<BatsmanPerformance> battingDetails;

}
