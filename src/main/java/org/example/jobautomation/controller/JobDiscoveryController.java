package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobDiscoveryResponse;
import org.example.jobautomation.dto.JobSearchRequest;
import org.example.jobautomation.service.discovery.JobDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/jobs/discovery")
@RequiredArgsConstructor
public class JobDiscoveryController {
    private final JobDiscoveryService jobDiscoveryService;

    @PostMapping("/run")
    public ResponseEntity<JobDiscoveryResponse> runDiscovery(@RequestBody JobSearchRequest request) {
        return ResponseEntity.ok(jobDiscoveryService.discoverJobs(request));
    }
}
