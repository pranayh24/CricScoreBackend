package org.pranay.api.cricscorebackend.repositeries;

import org.pranay.api.cricscorebackend.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScorecardRepo extends JpaRepository<Scorecard, Integer> {
}
