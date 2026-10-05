package org.example.jobautomation.dto;

import lombok.Data;

@Data
public class JobMatchRequest {
    private JobSearchRequest jobSearchRequest;
    private Integer limit;
    private Integer minimumScore=30;  // numeric threshold (default 30)
    private Boolean includeRejected;
}
