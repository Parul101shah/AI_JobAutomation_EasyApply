package org.example.jobautomation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm 'UTC'", timezone = "UTC")
    private Instant postedAt;
}

