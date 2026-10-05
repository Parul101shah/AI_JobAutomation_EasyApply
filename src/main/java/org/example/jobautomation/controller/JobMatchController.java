package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobMatchRequest;
import org.example.jobautomation.dto.JobMatchResponseDto;
import org.example.jobautomation.service.match.JobMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs/match")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping("/{userId}")
    public ResponseEntity<JobMatchResponseDto> matchJobs(
            @PathVariable Long userId,
            @RequestBody JobMatchRequest request)
    {
        return ResponseEntity.ok(jobMatchService.matchJobs(userId, request));
    }
}
