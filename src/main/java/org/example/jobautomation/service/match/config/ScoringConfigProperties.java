package org.example.jobautomation.service.match.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "scoring")
@Data
public class ScoringConfigProperties {

    private Weights weights = new Weights();
    private Thresholds thresholds = new Thresholds();
    private Map<String, List<String>> roleAliases = new HashMap<>();
    private Map<String, List<String>> skillAliases = new HashMap<>();
    private Map<String, List<String>> keywordSignals = new HashMap<>();
    private Recency recency = new Recency();
    private Location location = new Location();

    @Data
    public static class Weights {
        private Integer roleMatch = 25;
        private Integer skillsMatch = 40;
        private Integer experienceMatch = 20;
        private Integer locationMatch = 10;
        private Integer recencyMatch = 5;
    }

    @Data
    public static class Thresholds {
        private Integer high = 70;
        private Integer medium = 50;
        private Integer low = 30;
        private Integer reject = 30;
    }

    @Data
    public static class Recency {
        private Integer veryRecentDays = 7;
        private Integer recentDays = 30;
    }

    @Data
    public static class Location {
        private List<String> remoteKeywords;
        private List<String> regionKeywords;
    }
}
