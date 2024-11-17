package org.pranay.api.cricscorebackend.entities;

public class ScoreDetails {
    public int runs;
    public int wickets;
    public double overs;

    public ScoreDetails(int runs, int wickets, double overs) {
        this.runs = runs;
        this.wickets = wickets;
        this.overs = overs;
    }
}