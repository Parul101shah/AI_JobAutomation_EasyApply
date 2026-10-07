package org.example.jobautomation.service.discovery;

import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobListingDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class ParallelBoardFetcher {

    private final ExecutorService executor;

    public ParallelBoardFetcher(@Qualifier("jobSourceFetchExecutor") ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Fetches every key (board/company) in parallel with a per-key timeout,
     * keeps partial results, then interleaves round-robin and applies the limit.
     */
    public List<JobListingDto> fetch(String label,
                                     List<String> keys,
                                     Function<String, List<JobListingDto>> fetchOne,
                                     int limit,
                                     long perKeyTimeoutSeconds) {

        // Phase 1: start everything at once (returns immediately)
        List<CompletableFuture<List<JobListingDto>>> futures = keys.stream()
                .map(key -> CompletableFuture
                        .supplyAsync(() -> fetchOne.apply(key), executor)
                        .orTimeout(perKeyTimeoutSeconds, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            // only THIS key is lost; the others keep their results
                            log.warn("{} '{}' failed or timed out: {}", label, key, ex.toString());
                            return List.<JobListingDto>of();
                        }))
                .toList();

        // Phase 2: collect (each future is already bounded by its own timeout)
        List<List<JobListingDto>> perKey = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Phase 3: merge fairly, then apply the limit
        List<JobListingDto> merged = interleave(perKey, limit);
        log.info("{} discovery complete. sources={}, returnedJobs={}", label, keys.size(), merged.size());
        return merged;
    }

    /** Takes job #1 from each board, then job #2 from each board, ... until limit. */
    private List<JobListingDto> interleave(List<List<JobListingDto>> lists, int limit) {
        List<JobListingDto> out = new ArrayList<>();
        int maxSize = lists.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < maxSize && out.size() < limit; i++) {
            for (List<JobListingDto> list : lists) {
                if (i < list.size()) {
                    out.add(list.get(i));
                    if (out.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return out;
    }
}