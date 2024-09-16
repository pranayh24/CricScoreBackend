package org.pranay.api.cricscorebackend.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Data
@Entity
@Table(name ="scorecards")
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int scorecardId;

    @ManyToOne
    @JoinColumn(name="match_id")
    private Match match;

    private List<Innings> inningsList;

}
