package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResponseDto {
    private Long userId;
    private Integer totalJobsEvaluated;
    private Integer totalMatched;
    private List<JobMatchResultDto> matches;
}
