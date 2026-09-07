package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {
    private Long userId;
    private List<String> keywords;
    private String location;
    private Integer limitPerSource;
}
