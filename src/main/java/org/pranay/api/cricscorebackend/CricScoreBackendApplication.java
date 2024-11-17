package org.pranay.api.cricscorebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CricScoreBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(CricScoreBackendApplication.class, args);

    }

}
