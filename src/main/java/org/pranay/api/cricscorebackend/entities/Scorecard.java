package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@Entity
@Table(name ="scorecards")
@NoArgsConstructor
@AllArgsConstructor
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int scorecardId;

    @ManyToOne
    @JoinColumn(name="matchId")
    private Match match;

    @OneToMany(mappedBy = "scorecard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Innings> inningsList = new ArrayList<>();

    // method to properly set up the relationship
    public void addInnings(Innings innings) {
        inningsList.add(innings);
        innings.setScorecard(this);
    }

    // method to remove an innings if needed
    public void removeInnings(Innings innings) {
        inningsList.remove(innings);
        innings.setScorecard(null);
    }

}
