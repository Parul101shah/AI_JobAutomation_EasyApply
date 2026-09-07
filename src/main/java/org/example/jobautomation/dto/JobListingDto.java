package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobListingDto {
    private JobSourceType source;
    private String externalJobId;
    private String title;
    private String company;
    private String location;
    private String jobUrl;
    private String description;
    private Instant postedAt;
}

