package org.pranay.api.cricscorebackend.controllers;

import org.pranay.api.cricscorebackend.services.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("match/charts")
public class ChartController {

    @Autowired
    private ChartService chartService;

    @GetMapping("/runs-progression/{matchId}")
    public ResponseEntity<byte[]> getRunsProgressChart(@PathVariable String matchId) {
        byte[] chartBbytes = chartService.generateRunsProgressionChart(matchId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition","inline","filename=runs-progression.png")
                .body(chartBbytes);
    }
}
