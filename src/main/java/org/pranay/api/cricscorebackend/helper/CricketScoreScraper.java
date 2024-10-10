package org.pranay.api.cricscorebackend.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class CricketScoreScraper {
    public static void main(String[] args) {
        try {
            // Load the document (replace with actual URL or HTML content)
            String url = "https://www.cricbuzz.com/live-cricket-scorecard/100247/ind-vs-ban-2nd-t20i-bangladesh-tour-of-india-2024"; // Example URL
            Document doc = Jsoup.connect(url).get();

            // Get the team innings and score
            Element inningsInfo = doc.selectFirst("div.cb-col.cb-scrd-hdr-rw");
            if (inningsInfo != null) {
                String teamName = inningsInfo.select("span").first() != null ? inningsInfo.select("span").first().text() : "Unknown";
                String teamScore = inningsInfo.select("span.pull-right") != null ? inningsInfo.select("span.pull-right").text() : "Unknown";

                System.out.println("Team: " + teamName + ", Score: " + teamScore);
            } else {
                System.out.println("Team or score information not found.");
            }

            // Get batter details
            Elements batterRows = doc.select("div.cb-col.cb-col-100.cb-scrd-itms");
            for (Element row : batterRows) {
                Element playerElement = row.selectFirst("a.cb-text-link");
                if (playerElement != null) {
                    String playerName = playerElement.text();
                    String dismissalInfo = row.select("div.cb-col-33 span.text-gray").text();

                    // Get all the other statistics (runs, balls, 4s, 6s, SR) safely
                    Elements stats = row.select("div.cb-col-8.text-right");

                    String runs = stats.size() > 0 ? stats.get(0).text() : "0";
                    String ballsFaced = stats.size() > 1 ? stats.get(1).text() : "0";
                    String fours = stats.size() > 2 ? stats.get(2).text() : "0";
                    String sixes = stats.size() > 3 ? stats.get(3).text() : "0";
                    String strikeRate = stats.size() > 4 ? stats.get(4).text() : "0";

                    System.out.println("Player: " + playerName + ", Dismissal: " + dismissalInfo +
                            ", Runs: " + runs + ", Balls: " + ballsFaced +
                            ", 4s: " + fours + ", 6s: " + sixes + ", SR: " + strikeRate);
                }
            }

            // Get bowling details with proper null and bounds checks
            System.out.println("\nBowling Info:");
            Elements bowlerRows = doc.select("div.cb-col.cb-col-100.cb-scrd-itms"); // Selector for bowlers section
            for (Element row : bowlerRows) {
                Elements bowlerDetails = row.select("div.cb-col");

                if (bowlerDetails != null && bowlerDetails.size() > 5) {
                    // Extract bowler details safely, ensuring no index-out-of-bounds or null pointer exceptions
                    String bowlerName = bowlerDetails.get(0) != null ? bowlerDetails.get(0).text() : "Unknown";
                    String overs = bowlerDetails.get(1) != null ? bowlerDetails.get(1).text() : "0";
                    String maidens = bowlerDetails.get(2) != null ? bowlerDetails.get(2).text() : "0";
                    String runsConceded = bowlerDetails.get(3) != null ? bowlerDetails.get(3).text() : "0";
                    String wickets = bowlerDetails.get(4) != null ? bowlerDetails.get(4).text() : "0";
                    String economy = bowlerDetails.get(5) != null ? bowlerDetails.get(5).text() : "0";

                    System.out.println("Bowler: " + bowlerName + ", Overs: " + overs + ", Maidens: " + maidens +
                            ", Runs: " + runsConceded + ", Wickets: " + wickets + ", Economy: " + economy);
                } else {
                    System.out.println("Incomplete bowler details or invalid row detected.");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Error: Trying to access an index that does not exist. Check your data structure.");
        } catch (NullPointerException e) {
            System.out.println("Error: Encountered a null value while extracting data. Check your HTML structure.");
        }
    }
}



    /*private static class Batsman {
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
    }*/
