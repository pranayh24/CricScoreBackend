package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@Table(name="innings")
@NoArgsConstructor
@AllArgsConstructor
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

    @ElementCollection
    private List<BatsmanPerformance> battingDetails;

    @ElementCollection
    private List<BowlerPerformance> bowlingDetails;

    private String status;



}
