package org.example.jobautomation.dto;

import lombok.Data;

@Data
public class JobMatchRequest {
    private JobSearchRequest jobSearchRequest;
    private Integer limit;
    private Boolean includeRejected;
}
