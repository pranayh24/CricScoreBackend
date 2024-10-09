package org.pranay.api.cricscorebackend.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.pranay.api.cricscorebackend.entities.*;
import org.pranay.api.cricscorebackend.repositeries.MatchRepo;
import org.pranay.api.cricscorebackend.repositeries.ScorecardRepo;
import org.pranay.api.cricscorebackend.services.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class MatchServiceImpl implements MatchService {
    @Autowired
    private MatchRepo matchRepo;
    @Autowired
    private final ScorecardRepo scorecardRepo;

    public MatchServiceImpl(MatchRepo matchRepo, ScorecardRepo scorecardRepo, EntityManager entityManager) {
        this.matchRepo = matchRepo;
        this.scorecardRepo = scorecardRepo;
        this.entityManager = entityManager;
    }

    @Override
    public List<Match> getLiveMatchScores() {
        List<Match> matches = new ArrayList<>();
        try {
            String url = "https://www.cricbuzz.com/cricket-match/live-scores";
            Document document = Jsoup.connect(url).get();
            Elements liveScoreElements = document.select("div.cb-mtch-lst.cb-tms-itm");
            for (Element match : liveScoreElements) {
                HashMap<String, String> liveMatchInfo = new LinkedHashMap<>();
                String teamsHeading = match.select("h3.cb-lv-scr-mtch-hdr").select("a").text();
                String matchNumberVenue = match.select("span").text();
                Elements matchBatTeamInfo = match.select("div.cb-hmscg-bat-txt");
                String battingTeam = matchBatTeamInfo.select("div.cb-hmscg-tm-nm").text();
                String score = matchBatTeamInfo.select("div.cb-hmscg-tm-nm+div").text();
                Elements bowlTeamInfo = match.select("div.cb-hmscg-bwl-txt");
                String bowlTeam = bowlTeamInfo.select("div.cb-hmscg-tm-nm").text();
                String bowlTeamScore = bowlTeamInfo.select("div.cb-hmscg-tm-nm+div").text();
                String textLive = match.select("div.cb-text-live").text();
                String textComplete = match.select("div.cb-text-complete").text();

                // Getting match link and handling missing links
                String matchLink = match.select("a.cb-lv-scrs-well.cb-lv-scrs-well-live").attr("href").toString();
                if (matchLink == null || matchLink.isEmpty()) {
                    System.out.println("No valid match link found for match: " + teamsHeading);
                    continue;  // Skip this match
                }

                Match match1 = new Match();
                match1.setTeamHeading(teamsHeading);
                match1.setMatchNumberVenue(matchNumberVenue);
                match1.setBattingTeam(battingTeam);
                match1.setBattingTeamScore(score);
                match1.setBowlingTeam(bowlTeam);
                match1.setBowlingTeamScore(bowlTeamScore);
                match1.setLiveText(textLive);
                match1.setMatchLink(matchLink);
                match1.setTextComplete(textComplete);
                match1.setMatchStatus();

                matches.add(match1);

                // Update the match in database
                updateMatch(match1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matches;
    }

    private void updateMatch(Match match1) {
        Match match = this.matchRepo.findByTeamHeading(match1.getTeamHeading()).orElse(null);
        if (match == null) {
            this.matchRepo.save(match1);
        } else {
            match1.setMatchId(match.getMatchId());
            this.matchRepo.save(match1);
        }
    }

    @Override
    public List<Match> getAllMatches() {
        return this.matchRepo.findAll();
    }

    public void fetchScorecards() {
        List<Match> matches = getAllMatches();
        for (Match match : matches) {
            // Check if match link is available and valid
            String matchLink = match.getMatchLink();
            if (matchLink != null && !matchLink.isEmpty()) {
                try {
                    // Call scrapScorecard method for each match with a valid match link
                    scrapScorecard("https://www.cricbuzz.com" + matchLink, match); // Assuming relative URL, prepend base URL
                } catch (IOException e) {
                    System.err.println("Failed to scrape scorecard for match: " + match.getTeamHeading());
                    e.printStackTrace();
                }
            } else {
                System.out.println("No valid match link found for match: " + match.getTeamHeading());
            }
        }
    }
    @Autowired
    private EntityManager entityManager;

    @Transactional
    public void scrapScorecard(String matchLink, Match match) throws IOException {
        // Connect to the scorecard page
        Document scorecardDoc = Jsoup.connect(matchLink).get();

        // Create a new Scorecard entity or fetch existing one
        Scorecard scorecard = scorecardRepo.findByMatch(match)
                .orElse(new Scorecard());
        scorecard.setMatch(match);

        // Clear existing innings list to avoid duplicates
        scorecard.getInningsList().clear();

        // List to hold innings data
        List<Innings> inningsList = new ArrayList<>();

        // Select the innings container
        Elements inningsElements = scorecardDoc.select("div.cb-col.cb-col-100.cb-ltst-wgt-hdr");

        for (Element inningsElement : inningsElements) {
            Innings innings = new Innings();
            innings.setScorecard(scorecard);  // Set the scorecard reference
            // Scrape the team name
            Element teamNameElement = inningsElement.selectFirst("div.cb-col.cb-col-100.cb-scrd-hdr-rw span");
            String teamName = teamNameElement != null ? teamNameElement.text() : "Unknown Team";
            innings.setTeamName(teamName);

            // Scrape the fall of wickets
            String matchDetails = inningsElement.select("div.cb-nav-subhdr").text();
            String[] detailsArray = matchDetails.split(","); // Split by commas to extract the details

            if (detailsArray.length > 1) {
                innings.setFallOfWickets(detailsArray[1].trim()); // Fall of wickets
                String runsWickets = detailsArray[0].trim(); // Something like "100/2"
                String[] runsWicketsSplit = runsWickets.split("/");

                if (runsWicketsSplit.length > 1) {
                    innings.setRunsScored(parseIntSafe(runsWicketsSplit[0])); // Runs
                    innings.setWicketsLost(parseIntSafe(runsWicketsSplit[1])); // Wickets
                } else {
                    innings.setRunsScored(parseIntSafe(runsWickets)); // Set runs and assume 0 wickets
                    innings.setWicketsLost(0); // Default to 0 wickets if not available
                }

                innings.setOverBowled(detailsArray[1].trim()); // Overs
            }

            // Scrape batsman performance (use your actual selector)
            List<BatsmanPerformance> batsmanPerformances = new ArrayList<>();
            Elements batsmen = inningsElement.select("div.batsman-info");  // Adjust based on your HTML
            for (Element batsman : batsmen) {
                BatsmanPerformance batsmanPerformance = new BatsmanPerformance();
                batsmanPerformance.setBatsmanName(batsman.select("a.cb-text-link").text());
                batsmanPerformance.setRunsScored(parseIntSafe(batsman.select("div.cb-col-8.text-right.text-bold").text()));
                batsmanPerformance.setBallsFaced(parseIntSafe(batsman.select("div.cb-col-8.text-right").size() > 1 ? batsman.select("div.cb-col-8.text-right").get(1).text() : "0"));
                batsmanPerformance.setFours(parseIntSafe(batsman.select("div.cb-col-8.text-right").size() > 2 ? batsman.select("div.cb-col-8.text-right").get(2).text() : "0"));
                batsmanPerformance.setSixes(parseIntSafe(batsman.select("div.cb-col-8.text-right").size() > 3 ? batsman.select("div.cb-col-8.text-right").get(3).text() : "0"));
                batsmanPerformance.setDismissalInfo(batsman.select("span.text-gray").text());

                batsmanPerformances.add(batsmanPerformance);
            }
            innings.setBattingDetails(batsmanPerformances);

            // Scrape bowler performance (use your actual selector)
            List<BowlerPerformance> bowlerPerformances = new ArrayList<>();
            Elements bowlers = inningsElement.select("div.bowler-info"); // Adjust based on your HTML
            for (Element bowler : bowlers) {
                BowlerPerformance bowlerPerformance = new BowlerPerformance();
                bowlerPerformance.setBowlerName(bowler.select("span.bowler-name").text());
                bowlerPerformance.setOverBowled(bowler.select("span.overs").text());
                bowlerPerformance.setRunsScored(parseIntSafe(bowler.select("span.runs").text()));
                bowlerPerformance.setWicketsTaken(parseIntSafe(bowler.select("span.wickets").text()));
                bowlerPerformance.setEconomyRate(parseDoubleSafe(bowler.select("span.economy").text()));

                bowlerPerformances.add(bowlerPerformance);
            }
            innings.setBowlingDetails(bowlerPerformances);

            scorecard.getInningsList().add(innings);
        }

        // Clear the persistence context to avoid any stale data
        entityManager.clear();

        // Save or update the scorecard
        scorecardRepo.save(scorecard);
    }


    // Helper method for safe Integer parsing
    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Helper method for safe Double parsing
    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

}
