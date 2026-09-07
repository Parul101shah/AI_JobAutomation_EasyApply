package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDiscoveryResponse {
    private int totalJobs;
    private List<JobListingDto> jobs;
    private Map<JobSourceType, Integer> sourceCounts;
}
