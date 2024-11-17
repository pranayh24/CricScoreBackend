package org.pranay.api.cricscorebackend.services;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;
import org.pranay.api.cricscorebackend.entities.Match;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChartService {
    private Map<String, List<InningsScore>> matchScoreHistory = new HashMap<>();

    private static class InningsScore {
        int inningsNumber;
        int runs;
        boolean declared;
        boolean followOn;
        String teamName;
        long timestamp;

        InningsScore(int inningsNumber, int runs, boolean declared, boolean followOn, String teamName) {
            this.inningsNumber = inningsNumber;
            this.runs = runs;
            this.declared = declared;
            this.followOn = followOn;
            this.teamName = teamName;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public void updateMatchScore(Match match) {
        // Get team names without dots
        String battingTeam = match.getBattingTeam().replace(".", "").trim();
        String bowlingTeam = match.getBowlingTeam().replace(".", "").trim();

        // Parse batting team's score(s)
        List<InningsScore> battingScores = parseMultiInningsScore(match.getBattingTeamScore(), battingTeam);
        List<InningsScore> bowlingScores = parseMultiInningsScore(match.getBowlingTeamScore(), bowlingTeam);

        // Update history
        String matchKey = String.valueOf(match.getMatchId());
        List<InningsScore> matchScores = matchScoreHistory.computeIfAbsent(matchKey, k -> new ArrayList<>());

        // Update or add new scores
        updateInningsScores(matchScores, battingScores);
        updateInningsScores(matchScores, bowlingScores);
    }

    private void updateInningsScores(List<InningsScore> existingScores, List<InningsScore> newScores) {
        for (InningsScore newScore : newScores) {
            boolean scoreExists = false;
            for (InningsScore existingScore : existingScores) {
                if (existingScore.teamName.equals(newScore.teamName) &&
                        existingScore.inningsNumber == newScore.inningsNumber) {
                    existingScore.runs = newScore.runs;
                    existingScore.declared = newScore.declared;
                    existingScore.followOn = newScore.followOn;
                    existingScore.timestamp = newScore.timestamp;
                    scoreExists = true;
                    break;
                }
            }
            if (!scoreExists) {
                existingScores.add(newScore);
            }
        }
    }

    private List<InningsScore> parseMultiInningsScore(String scoreStr, String teamName) {
        List<InningsScore> scores = new ArrayList<>();
        if (scoreStr == null || scoreStr.isEmpty()) {
            return scores;
        }

        // Split multiple innings scores (separated by &)
        String[] inningsScores = scoreStr.split("&");
        boolean followOn = scoreStr.contains("f/o");

        for (int i = 0; i < inningsScores.length; i++) {
            String innings = inningsScores[i].trim();

            // Parse each innings score
            Pattern pattern = Pattern.compile("(\\d+)(?:[-/](\\d+))?(\\s*d)?");
            Matcher matcher = pattern.matcher(innings);

            if (matcher.find()) {
                int runs = Integer.parseInt(matcher.group(1));
                boolean declared = matcher.group(3) != null; // Check for 'd' (declaration)

                scores.add(new InningsScore(i + 1, runs, declared, followOn, teamName));
            }
        }

        return scores;
    }

    public byte[] generateRunsProgressionChart(String matchId) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<InningsScore> matchScores = matchScoreHistory.get(matchId);
        if (matchScores == null || matchScores.isEmpty()) {
            return createEmptyChart();
        }

        // Group scores by team
        Map<String, List<InningsScore>> teamScores = new HashMap<>();
        for (InningsScore score : matchScores) {
            teamScores.computeIfAbsent(score.teamName, k -> new ArrayList<>()).add(score);
        }

        // Add data points for each team
        for (Map.Entry<String, List<InningsScore>> entry : teamScores.entrySet()) {
            String team = entry.getKey();
            List<InningsScore> scores = entry.getValue();

            for (InningsScore score : scores) {
                String label = "Innings " + score.inningsNumber +
                        (score.declared ? " (d)" : "") +
                        (score.followOn ? " (f/o)" : "");
                dataset.addValue(score.runs, team, label);
            }
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Match Progress",
                "Innings",
                "Runs",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeChart(chart);
        return chartToBytes(chart, 800, 400);
    }

    private byte[] createEmptyChart() {
        DefaultCategoryDataset emptyDataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createLineChart(
                "Match Progress (No Data Available)",
                "Innings",
                "Runs",
                emptyDataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        return chartToBytes(chart, 800, 400);
    }

    private void customizeChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();

        // Customize lines
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 102, 204));    // Blue for team 1
        renderer.setSeriesPaint(1, new Color(204, 0, 0));      // Red for team 2
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));

        // Add shapes to data points
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesVisible(1, true);

        plot.setRenderer(renderer);

        // Customize plot
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        // Customize title
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));

        // Customize legend
        chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 12));

        // Rotate x-axis labels if needed
        plot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.UP_45
        );
    }

    private byte[] chartToBytes(JFreeChart chart, int width, int height) {
        try {
            BufferedImage image = chart.createBufferedImage(width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error converting chart to image", e);
        }
    }
}