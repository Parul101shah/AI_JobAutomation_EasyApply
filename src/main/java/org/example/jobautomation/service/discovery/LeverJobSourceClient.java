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
public class LeverJobSourceClient implements JobSourceClient {

    private final RestTemplate restTemplate;
    private final ParallelBoardFetcher boardFetcher;
    private static final long COMPANY_TIMEOUT_SECONDS = 10;

    @Value("#{'${jobs.lever.companies:}'.split(',')}")
    private List<String> companyHandles;

    @Override
    public JobSourceType sourceType() {
        return JobSourceType.LEVER;
    }

    @Override
    public List<JobListingDto> searchJobs(JobSearchRequest request) {
        List<String> companies = sanitizeHandles(companyHandles);
        int limit = resolveLimit(request);

        if (companies.isEmpty()) {
            log.info("Lever discovery skipped. No company handles configured.");
            return new ArrayList<>();
        }

        return boardFetcher.fetch("Lever", companies,
                company -> fetchCompanyJobs(company, request, limit),
                limit, COMPANY_TIMEOUT_SECONDS);
    }

    private List<JobListingDto> fetchCompanyJobs(String company, JobSearchRequest request, int remainingSlots) {
        String url = UriComponentsBuilder
                .fromUriString("https://api.lever.co/v0/postings/{company}")
                .queryParam("mode", "json")
                .buildAndExpand(company)
                .toUriString();

        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List jobs = response.getBody();
            if (jobs == null || jobs.isEmpty()) {
                return Collections.emptyList();
            }

            List<JobListingDto> mapped = new ArrayList<>();
            for (Object item : jobs) {
                if (!(item instanceof Map<?, ?> rawJob)) {
                    continue;
                }

                JobListingDto job = toJobListing(rawJob, company);
                if (job != null && matchesFilters(job, request)) {
                    mapped.add(job);
                }

                if (mapped.size() >= remainingSlots) {
                    break;
                }
            }

            return mapped;
        } catch (RestClientException ex) {
            log.warn("Lever fetch failed for company {}: {}", company, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private JobListingDto toJobListing(Map<?, ?> rawJob, String company) {
        String id = valueAsString(rawJob.get("id"));
        String title = valueAsString(rawJob.get("text"));
        String jobUrl = valueAsString(rawJob.get("hostedUrl"));

        String description = valueAsString(rawJob.get("descriptionPlain"));
        if (!StringUtils.hasText(description)) {
            description = valueAsString(rawJob.get("description"));
        }

        String location = "";
        Object categoriesObject = rawJob.get("categories");
        if (categoriesObject instanceof Map<?, ?> categories) {
            location = valueAsString(categories.get("location"));
        }

        Instant postedAt = parseEpochMillis(rawJob.get("createdAt"));

        return new JobListingDto(
                JobSourceType.LEVER,
                id,
                title,
                company,
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

    private List<String> sanitizeHandles(List<String> handles) {
        if (handles == null) {
            return Collections.emptyList();
        }
        return handles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Instant parseEpochMillis(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long epochMillis = value instanceof Number
                    ? ((Number) value).longValue()
                    : Long.parseLong(String.valueOf(value));
            return Instant.ofEpochMilli(epochMillis);
        } catch (Exception ignored) {
            return null;
        }
    }
}