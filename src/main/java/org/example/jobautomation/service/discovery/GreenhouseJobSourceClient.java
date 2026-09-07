package org.example.jobautomation.service.discovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobSearchRequest;
import org.example.jobautomation.dto.JobSourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreenhouseJobSourceClient implements JobSourceClient {

    private final RestTemplate restTemplate;

    @Value("#{'${jobs.greenhouse.boards:}'.split(',')}")
    private List<String> boardTokens;

    @Override
    public JobSourceType sourceType() {
        return JobSourceType.GREENHOUSE;
    }

    @Override
    public List<JobListingDto> searchJobs(JobSearchRequest request) {
        List<JobListingDto> results = new ArrayList<>();
        List<String> boards = sanitizeTokens(boardTokens);
        int limit = resolveLimit(request);

        if (boards.isEmpty()) {
            log.info("Greenhouse discovery skipped. No board tokens configured.");
            return results;
        }

        for (String board : boards) {
            if (results.size() >= limit) {
                break;
            }
            results.addAll(fetchBoardJobs(board, request, limit - results.size()));
        }

        log.info("Greenhouse discovery complete. boards={}, returnedJobs={}", boards.size(), results.size());
        return results;
    }

    private List<JobListingDto> fetchBoardJobs(String board, JobSearchRequest request, int remainingSlots) {
        String url = UriComponentsBuilder
                .fromUriString("https://boards-api.greenhouse.io/v1/boards/{board}/jobs")
                .queryParam("content", "true")
                .buildAndExpand(board)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Object jobsObject = response.getBody() == null ? null : response.getBody().get("jobs");
            if (!(jobsObject instanceof List<?> jobs)) {
                return Collections.emptyList();
            }

            List<JobListingDto> mapped = new ArrayList<>();
            for (Object item : jobs) {
                if (!(item instanceof Map<?, ?> rawJob)) {
                    continue;
                }

                JobListingDto job = toJobListing(rawJob, board);
                if (job != null && matchesFilters(job, request)) {
                    mapped.add(job);
                }

                if (mapped.size() >= remainingSlots) {
                    break;
                }
            }

            return mapped;
        } catch (RestClientException ex) {
            log.warn("Greenhouse fetch failed for board {}: {}", board, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private JobListingDto toJobListing(Map<?, ?> rawJob, String board) {
        String id = valueAsString(rawJob.get("id"));
        String title = valueAsString(rawJob.get("title"));
        String jobUrl = valueAsString(rawJob.get("absolute_url"));
        String description = valueAsString(rawJob.get("content"));
        String postedRaw = valueAsString(rawJob.get("updated_at"));

        String location = "";
        Object locationObject = rawJob.get("location");
        if (locationObject instanceof Map<?, ?> locationMap) {
            location = valueAsString(locationMap.get("name"));
        }

        Instant postedAt = parseInstant(postedRaw);

        return new JobListingDto(
                JobSourceType.GREENHOUSE,
                id,
                title,
                board,
                location,
                jobUrl,
                description,
                postedAt
        );
    }

    private boolean matchesFilters(JobListingDto job, JobSearchRequest request) {
        if (request == null) {
            return true;
        }

        if (StringUtils.hasText(request.getLocation())) {
            String wantedLocation = request.getLocation().toLowerCase();
            String actualLocation = valueAsString(job.getLocation()).toLowerCase();
            if (!actualLocation.contains(wantedLocation)) {
                return false;
            }
        }

        List<String> keywords = request.getKeywords() == null ? Collections.emptyList() : request.getKeywords();
        List<String> normalizedKeywords = keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (normalizedKeywords.isEmpty()) {
            return true;
        }

        String haystack = (valueAsString(job.getTitle()) + " " + valueAsString(job.getDescription())).toLowerCase();
        return normalizedKeywords.stream().anyMatch(haystack::contains);
    }

    private int resolveLimit(JobSearchRequest request) {
        if (request == null || request.getLimitPerSource() == null || request.getLimitPerSource() <= 0) {
            return 25;
        }
        return request.getLimitPerSource();
    }

    private List<String> sanitizeTokens(List<String> tokens) {
        if (tokens == null) {
            return Collections.emptyList();
        }
        return tokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}