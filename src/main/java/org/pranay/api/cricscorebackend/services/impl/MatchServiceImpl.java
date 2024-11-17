package org.pranay.api.cricscorebackend.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.pranay.api.cricscorebackend.entities.*;
import org.pranay.api.cricscorebackend.repositeries.MatchRepo;
import org.pranay.api.cricscorebackend.repositeries.ScorecardRepo;
import org.pranay.api.cricscorebackend.services.ChartService;
import org.pranay.api.cricscorebackend.services.MatchService;
import org.pranay.api.cricscorebackend.services.PredictionIntegrationService;
import org.pranay.api.cricscorebackend.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final WebSocketHandler webSocketHandler;

    @Autowired
    private final EntityManager entityManager;
    @Autowired
    private MatchRepo matchRepo;
    @Autowired
    private final ScorecardRepo scorecardRepo;
    @Autowired
    private ChartService chartService;

    @Autowired
    private PredictionIntegrationService predictionIntegrationService;

    public MatchServiceImpl(WebSocketHandler webSocketHandler, MatchRepo matchRepo, ScorecardRepo scorecardRepo, EntityManager entityManager) {
        this.webSocketHandler = webSocketHandler;
        this.matchRepo = matchRepo;
        this.scorecardRepo = scorecardRepo;
        this.entityManager = entityManager;
    }

    @Override
    @Scheduled(fixedRate = 30000)
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

                // Extract match format from the match heading or number
                String matchFormat = determineMatchFormat(teamsHeading, matchNumberVenue);

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
                match1.setMatchFormat(matchFormat); // Set the match format
                match1.setMatchStatus();

                matches.add(match1);

                // Update the charts
                chartService.updateMatchScore(match1);
                webSocketHandler.sendMessageToAll(match1.toString());
                // Update the match in database
                updateMatch(match1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matches;
    }

    private String determineMatchFormat(String teamsHeading, String matchNumberVenue) {
        String combinedText = (teamsHeading + " " + matchNumberVenue).toLowerCase();

        if (combinedText.contains(" t20 ") || combinedText.contains("twenty20") || combinedText.contains("t20i")) {
            return "T20";
        } else if (combinedText.contains(" odi ") || combinedText.contains("one-day") || combinedText.contains("1st odi")) {
            return "ODI";
        } else if (combinedText.contains(" test ") || combinedText.contains("day series") || combinedText.contains("unofficial test")) {
            return "TEST";
        } else {
            // Check for additional tournament-specific formats
            if (combinedText.contains("wbbl") || combinedText.contains("bbl")) {
                return "T20";
            } else if (combinedText.contains("one day cup") || combinedText.contains("list a")) {
                return "ODI";
            } else if (combinedText.contains("first-class") || combinedText.contains("sheffield shield")) {
                return "TEST";
            }
        }
        return "UNKNOWN"; // Default case if format cannot be determined
    }

    private void updateMatch(Match match1) {
        Match match = this.matchRepo.findByTeamHeading(match1.getTeamHeading()).orElse(null);
        if (match == null) {
            this.matchRepo.save(match1);
        } else {
            match1.setMatchId(match.getMatchId());
            this.matchRepo.save(match1);
        }
        predictionIntegrationService.processPredictionForMatch(match1);
    }

    @Override
    public List<Match> getAllMatches() {
        return this.matchRepo.findAll();
    }
    @Override
    public Match getMatchById(int matchId) {
        return matchRepo.findById(matchId).orElse(null);
    }
}