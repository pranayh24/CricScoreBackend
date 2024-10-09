package org.pranay.api.cricscorebackend.entities;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BowlerPerformance {

    private String bowlerName;
    private String overBowled;
    private int runsScored;
    private int wicketsTaken;
    private double economyRate;
}
