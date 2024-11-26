package org.pranay.api.cricscorebackend.controllers;

import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.services.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService matchService;
    private static final Logger logger = LoggerFactory.getLogger(MatchController.class);

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/live")
    public ResponseEntity<List<Match>> getLiveMatches() {
        return new ResponseEntity<>(this.matchService.getLiveMatchScores(), HttpStatus.OK);
    }

    @PostMapping("/updateTossInfo/{matchId}")
    public ResponseEntity<String> updateTossInfo(@PathVariable int matchId, @RequestBody Map<String, String> tossInfo) {
        try {
            Match match = matchService.getMatchById(matchId);
            if (match == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Match not found");
            }

            // Update match data with manual toss info
            match.setTossWinner(tossInfo.get("toss_winner"));
            match.setTossDecision(tossInfo.get("toss_decision"));
            matchService.saveMatch(match);

            return ResponseEntity.ok("Toss information updated successfully");
        } catch (Exception e) {
            logger.error("Error updating toss information: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating toss information");
        }
    }
}