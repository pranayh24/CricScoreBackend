package org.pranay.api.cricscorebackend.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CricketScoreScraper {
    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect("https://www.cricbuzz.com/live-cricket-scorecard/100229/ban-vs-ind-2nd-test-bangladesh-tour-of-india-2024").get();
            Elements inningsSections = doc.select("div.cb-col.cb-col-100.cb-ltst-wgt-hdr");
            int i=0;
            for (Element inningsSection : inningsSections) {
                printBattingSummary(inningsSection);
                printBowlingSummary(inningsSection);
                System.out.println("-------------------------"+i++);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printBattingSummary(Element inningsSection) {
        System.out.println("Batting Summary");

        Element teamNameElement = inningsSection.selectFirst("div.cb-col.cb-col-100.cb-scrd-hdr-rw span");
        String teamName = teamNameElement != null ? teamNameElement.text() : "Team not found";

        Element scoreElement = inningsSection.selectFirst("span.pull-right");
        String score = scoreElement != null ? scoreElement.text() : "Score not found";

        System.out.println("Team: " + teamName + ", Score: " + score);

        Elements batsmenElements = inningsSection.select("div.cb-col.cb-col-100.cb-scrd-itms");
        for (Element batsmanElement : batsmenElements) {
            Elements nameElement = batsmanElement.select("a.cb-text-link");
            if (!nameElement.isEmpty()) {
                Batsman batsman = new Batsman();
                batsman.name = nameElement.text();
                batsman.dismissal = batsmanElement.select("span.text-gray").text();
                Elements stats = batsmanElement.select("div.cb-col.cb-col-8.text-right");

                if (stats.size() >= 5) {
                    batsman.runs = stats.get(0).text();
                    batsman.balls = stats.get(1).text();
                    batsman.fours = stats.get(2).text();
                    batsman.sixes = stats.get(3).text();
                    batsman.strikeRate = stats.get(4).text();
                    System.out.println(batsman);
                }
            }
        }
    }

    private static void printBowlingSummary(Element inningsSection) {
        System.out.println("\n--- Bowling Summary ---");
        Elements bowlerRows = inningsSection.select("div.cb-col.cb-col-100.cb-scrd-itms");

        for (Element bowlerRow : bowlerRows) {
            Elements nameElement = bowlerRow.select("a.cb-text-link");
            if (!nameElement.isEmpty()) {
                Bowler bowler = new Bowler();
                bowler.name = nameElement.text();

                Elements stats = bowlerRow.select("div.cb-col.cb-col-8.text-right, div.cb-col.cb-col-10.text-right");

                if (stats.size() == 7) {
                    bowler.overs = stats.get(0).text();
                    bowler.maidens = stats.get(1).text();
                    bowler.runs = stats.get(2).text();
                    bowler.wickets = stats.get(3).text();
                    bowler.noBalls = stats.get(4).text();
                    bowler.wides = stats.get(5).text();
                    bowler.economy = stats.get(6).text();

                    System.out.println(bowler);
                }
            }
        }
    }

    private static class Batsman {
        String name, dismissal, runs, balls, fours, sixes, strikeRate;

        @Override
        public String toString() {
            return String.format("Batsman: %s, Dismissal: %s, Runs: %s, Balls: %s, 4s: %s, 6s: %s, SR: %s",
                    name, dismissal, runs, balls, fours, sixes, strikeRate);
        }
    }

    private static class Bowler {
        String name, overs, maidens, runs, wickets, noBalls, wides, economy;

        @Override
        public String toString() {
            return String.format("Bowler: %s, Overs: %s, Maidens: %s, Runs: %s, Wickets: %s, No Balls: %s, Wides: %s, Economy: %s",
                    name, overs, maidens, runs, wickets, noBalls, wides, economy);
        }
    }
}