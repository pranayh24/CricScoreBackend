package org.pranay.api.cricscorebackend.controllers;
import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.services.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }
    @GetMapping("/live")
    public ResponseEntity<List<Match>> getLiveMatches() {
        return new ResponseEntity<>(this.matchService.getLiveMatchScores(), HttpStatus.OK);
    }
    @GetMapping("/update-scores")
    public ResponseEntity<String> updateScores() {
        try {
            matchService.fetchScorecards(); // Call the method to fetch scorecards from the cricket API
            return new ResponseEntity<>("Scores updated successfully!", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace(); // Consider logging this properly
            return new ResponseEntity<>("Failed to update scores.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
