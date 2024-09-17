package org.pranay.api.cricscorebackend.repositeries;

import org.pranay.api.cricscorebackend.entities.Innings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InningsRepo extends JpaRepository<Innings, Integer> {
}
