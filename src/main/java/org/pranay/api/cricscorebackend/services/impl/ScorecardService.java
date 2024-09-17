package org.pranay.api.cricscorebackend.services.impl;

import org.pranay.api.cricscorebackend.entities.Scorecard;
import org.pranay.api.cricscorebackend.repositeries.ScorecardRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ScorecardService {

    @Autowired
    private ScorecardRepo scorecardRepo;

    public Scorecard saveOrUpdateScorecard(Scorecard scorecard) {
        return scorecardRepo.save(scorecard);
    }

    public Optional<Scorecard> getScorecardByMatchId(int id) {
        return scorecardRepo.findById(id);
    }
}
