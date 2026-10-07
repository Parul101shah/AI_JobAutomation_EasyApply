package org.example.jobautomation.dto;

public record HighMatchDto(
        String token, String title, String company, String location, String jobUrl,
        int matchScore, int keywordScore, int aiScore, String aiSummary) {}
