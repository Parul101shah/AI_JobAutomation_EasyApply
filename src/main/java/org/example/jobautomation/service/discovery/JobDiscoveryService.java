package org.example.jobautomation.service.discovery;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobDiscoveryResponse;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobSearchRequest;
import org.example.jobautomation.dto.JobSourceType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobDiscoveryService {

    private final List<JobSourceClient> sourceClients;

    public JobDiscoveryResponse discoverJobs(JobSearchRequest request) {
        List<JobListingDto> allJobs = new ArrayList<>();
        Map<JobSourceType, Integer> sourceCounts = new EnumMap<>(JobSourceType.class);

        for (JobSourceClient client : sourceClients) {
            List<JobListingDto> jobsFromSource;
            try {
                jobsFromSource = client.searchJobs(request);
            } catch (Exception ex) {
                jobsFromSource = Collections.emptyList(); // fail-open for one source
            }
            allJobs.addAll(jobsFromSource);
            sourceCounts.put(client.sourceType(), jobsFromSource.size());
        }

        List<JobListingDto> deduped = deduplicate(allJobs);

        return new JobDiscoveryResponse(
                deduped.size(),
                deduped,
                sourceCounts
        );
    }

    private List<JobListingDto> deduplicate(List<JobListingDto> jobs) {
        Map<String, JobListingDto> unique = new LinkedHashMap<>();
        for (JobListingDto job : jobs) {
            String key = job.getSource() + "::" + safe(job.getExternalJobId()) + "::" + safe(job.getJobUrl());
            unique.putIfAbsent(key, job);
        }
        return unique.values().stream().collect(Collectors.toList());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

