package org.pranay.api.cricscorebackend.repositeries;

import org.pranay.api.cricscorebackend.entities.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRepo extends JpaRepository<Match, Integer> {
    // fetching match by providing team's name
    Optional<Match> findByTeamHeading(String teamHeading);
}
