package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatsmanPerformance {

    private String batsmanName;
    private int runsScored;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private int strikeRate;
    private String dismissalInfo;

}
