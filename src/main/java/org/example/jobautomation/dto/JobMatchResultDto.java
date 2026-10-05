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
    private Integer matchScore;
    private String matchLevel;
    private List<String> reasons;
    private List<String> concerns;
}
