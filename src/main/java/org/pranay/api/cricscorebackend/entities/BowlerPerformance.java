package org.pranay.api.cricscorebackend.entities;


import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class BowlerPerformance {

    private String bowlerName;
    private String overBowled;
    private int runsScored;
    private int wicketsTaken;
    private double economyRate;
}
