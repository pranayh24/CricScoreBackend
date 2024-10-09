package org.pranay.api.cricscorebackend.repositeries;

import org.pranay.api.cricscorebackend.entities.Match;
import org.pranay.api.cricscorebackend.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScorecardRepo extends JpaRepository<Scorecard, Integer> {
    Optional<Scorecard> findByMatch(Match match);
}
