package org.example.jobautomation.service.discovery;

import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobSearchRequest;
import org.example.jobautomation.dto.JobSourceType;

import java.util.List;

public interface JobSourceClient {
    JobSourceType sourceType();
    List<JobListingDto> searchJobs(JobSearchRequest request);
}
