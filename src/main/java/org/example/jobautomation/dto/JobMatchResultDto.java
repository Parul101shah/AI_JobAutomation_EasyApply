package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResultDto {
    private JobListingDto job;
    private Integer matchScore;     // final combined score
    private Integer keywordScore;   // rules-based
    private Integer aiScore;        // AI-based
    private String aiSummary;       // semantic explanation
    private String matchLevel;
    private List<String> reasons;
    private List<String> concerns;
}
