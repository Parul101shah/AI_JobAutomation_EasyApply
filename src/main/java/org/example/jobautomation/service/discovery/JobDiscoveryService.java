package org.example.jobautomation.service.discovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobDiscoveryResponse;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobSearchRequest;
import org.example.jobautomation.dto.JobSourceType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDiscoveryService {

    private final ExecutorService jobDiscoveryExecutor;
    private record SourceResult(JobSourceType jobSourceType, List<JobListingDto> jobs) {}
    private final List<JobSourceClient> sourceClients;

    public JobDiscoveryResponse discoverJobs(JobSearchRequest request) {
        List<JobListingDto> allJobs = new ArrayList<>();
        Map<JobSourceType, Integer> sourceCounts = new EnumMap<>(JobSourceType.class);

//        for (JobSourceClient client : sourceClients) {
//            List<JobListingDto> jobsFromSource;
//            try {
//                jobsFromSource = client.searchJobs(request);
//            } catch (Exception ex) {
//                jobsFromSource = Collections.emptyList(); // fail-open for one source
//            }
//            allJobs.addAll(jobsFromSource);
//            sourceCounts.put(client.sourceType(), jobsFromSource.size());
//        }

        // Phase 1: start every source in parallel (doesn't wait)
        List<CompletableFuture<SourceResult>> futures =sourceClients.stream()
                .map(client -> CompletableFuture.supplyAsync(
                        ()-> new SourceResult(client.sourceType(),client.searchJobs(request)),jobDiscoveryExecutor)
                        .orTimeout(10, TimeUnit.SECONDS)
                        .exceptionally(ex->{
                            log.warn("Job source {} failed: {}", client.sourceType(), ex.toString());
                            return new SourceResult(client.sourceType(), List.of());
                        }))
                .toList();


        // Phase 2: wait for results and collect
        for (CompletableFuture<SourceResult> future : futures) {
            SourceResult result = future.join();  //join waits until task is completed
            allJobs.addAll(result.jobs());
            sourceCounts.put(result.jobSourceType(), result.jobs().size());
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

