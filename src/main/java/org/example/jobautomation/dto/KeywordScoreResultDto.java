package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordScoreResultDto {
    private Integer totalScore;
    private Integer roleScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer locationScore;
    private Integer recencyScore;
    private List<String> reasons = new ArrayList<>();
    private List<String> concerns = new ArrayList<>();
}

