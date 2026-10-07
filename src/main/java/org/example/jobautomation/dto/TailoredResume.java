package org.example.jobautomation.dto;

import java.util.List;

public record TailoredResume(
        String fullName, String email, String phone, String location,
        String headline, String summary,
        List<String> skills,
        List<Experience> experience,
        List<Project> projects,
        List<String> education,
        List<String> certifications) {

    public record Experience(String title, String company, String period, List<String> bullets) {}
    public record Project(String name, String techStack, List<String> bullets) {}
}
