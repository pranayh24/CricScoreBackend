package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class BatsmanPerformance {

    private String batsmanName;
    private int runsScored;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private int strikeRate;
    private String dismissalInfo;

}
