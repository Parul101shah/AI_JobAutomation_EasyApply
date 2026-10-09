package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobMatchRequest;
import org.example.jobautomation.dto.JobMatchResponseDto;
import org.example.jobautomation.service.match.JobMatchService;
import org.example.jobautomation.service.notification.MatchNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;

@RestController
@Slf4j
@RequestMapping("/api/jobs/match")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;
    private final MatchNotificationService notificationService;
    private final ExecutorService jobScoringExecutor;

    @PostMapping("/{userId}")
    public ResponseEntity<JobMatchResponseDto> matchJobs(@PathVariable Long userId,
                                                         @RequestBody JobMatchRequest request) {
        return ResponseEntity.ok(jobMatchService.matchJobs(userId, request));
    }

    /** Runs in the background; HIGH matches are emailed to the registered address. */
    @PostMapping("/{userId}/notify")
    public ResponseEntity<Map<String, String>> notifyHigh(@PathVariable Long userId,
                                                          @RequestBody JobMatchRequest request) {
        long start = System.currentTimeMillis();
        log.info("notifyHigh request accepted for userId={}", userId);

        jobScoringExecutor.submit(() -> {
            long taskStart = System.currentTimeMillis();
            try {
                notificationService.notifyHighMatches(userId, request);
                log.info("notifyHigh async task finished in {} ms for userId={}",
                        System.currentTimeMillis() - taskStart, userId);
            } catch (Exception e) {
                log.error("notifyHigh async task failed for userId={}", userId, e);
            }
        });

        log.info("notifyHigh endpoint returned in {} ms for userId={}",
                System.currentTimeMillis() - start, userId);

        return ResponseEntity.accepted()
                .body(Map.of("status", "Processing. HIGH matches will be emailed to the registered address."));
    }
}
