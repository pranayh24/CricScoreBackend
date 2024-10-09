package org.pranay.api.cricscorebackend.services;

import org.pranay.api.cricscorebackend.entities.Match;

import java.util.List;

public interface MatchService {
    //get all matches
    //get live matches
    List<Match> getAllMatches();
    List<Match> getLiveMatchScores();
    void fetchScorecards();
}
