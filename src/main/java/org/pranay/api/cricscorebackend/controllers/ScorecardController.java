//package org.pranay.api.cricscorebackend.controllers;
//
//import org.pranay.api.cricscorebackend.entities.Scorecard;
//import org.pranay.api.cricscorebackend.services.ScorecardService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Optional;
//
//@RestController
//public class ScorecardController {
//
//    @Autowired
//    private ScorecardService scorecardService;
//
//    public ResponseEntity<Scorecard> getScorecardById(int id) {
//        Optional<Scorecard> scorecard = scorecardService.getScorecardByMatchId(id);
//        return scorecard.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
//    }
//}
