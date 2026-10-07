package org.example.jobautomation.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiScoreResultDto {
    private Integer score;
    private String matchLevel;
    private String summary;
    private List<String> reasons = new ArrayList<>();
    private List<String> concerns = new ArrayList<>();
}
