package org.example.jobautomation.service.apply;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.entity.UserProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AnswerResolver {

    private final ChatClient.Builder chatClientBuilder;

    /** Sensitive topics: never auto-answer beyond "decline". */
    private static final List<String> EEO = List.of("gender", "race", "ethnic", "veteran",
            "disability", "sexual orientation", "pronoun", "hispanic");

    public enum Kind { TEXT, YES_NO, SELECT, TEXTAREA }

    /** @param options visible option labels for SELECT/radio, else empty */
    public Optional<String> resolve(String label, Kind kind, List<String> options,
                                    UserProfile u, JobListingDto job) {
        String l = label == null ? "" : label.toLowerCase().trim();

        if (EEO.stream().anyMatch(l::contains)) {
            return options.stream()
                    .filter(o -> o.toLowerCase().matches(".*(decline|prefer not|do not wish|don't wish).*"))
                    .findFirst();                        // empty -> manual
        }

        // 1. Deterministic profile mapping
        if (l.contains("linkedin"))                      return opt(u.getLinkedinUrl());
        if (l.contains("phone") || l.contains("mobile")) return opt(u.getPhone());
        if (l.contains("current location") || l.equals("city") || l.contains("where are you located"))
            return opt(u.getCurrentLocation() != null ? u.getCurrentLocation() : u.getPreferredLocation());
        if (l.contains("years of experience") && u.getTotalExperienceYears() != null)
            return Optional.of(String.valueOf(u.getTotalExperienceYears()));
        if (l.contains("notice") && u.getNoticePeriodDays() != null)
            return Optional.of(String.valueOf(u.getNoticePeriodDays()));
        if ((l.contains("salary") || l.contains("compensation")) && u.getExpectedSalary() != null)
            return Optional.of(String.valueOf(u.getExpectedSalary()));
        if (l.contains("sponsor"))
            return yesNo(u.getRequiresSponsorship(), options);
        if (l.contains("authorized") || l.contains("authorised") || l.contains("legally") || l.contains("right to work"))
            return yesNo(u.getWorkAuthorized(), options);
        if (l.contains("website") || l.contains("portfolio") || l.contains("github")) return Optional.empty();

        // 2. AI only for open-ended text about the candidate's background
        if (kind == Kind.TEXTAREA || (kind == Kind.TEXT && l.contains("why"))) {
            return Optional.ofNullable(aiAnswer(label, u, job));
        }
        return Optional.empty();
    }

    private Optional<String> yesNo(Boolean v, List<String> options) {
        if (v == null) return Optional.empty();
        String want = v ? "yes" : "no";
        if (options.isEmpty()) return Optional.of(want.substring(0, 1).toUpperCase() + want.substring(1));
        return options.stream().filter(o -> o.toLowerCase().startsWith(want)).findFirst();
    }

    private Optional<String> opt(String s) { return s == null || s.isBlank() ? Optional.empty() : Optional.of(s); }

    private String aiAnswer(String question, UserProfile u, JobListingDto job) {
        String jd = job.getDescription() == null ? "" :
                job.getDescription().substring(0, Math.min(2000, job.getDescription().length()));
        String a = chatClientBuilder.build().prompt()
                .system("""
                        Answer a job application question in first person, max 120 words, plain text.
                        Use ONLY facts in the candidate profile. If the profile lacks the facts, reply exactly: UNKNOWN.
                        """)
                .user(x -> x.text("""
                        QUESTION: {q}
                        PROFILE: {summary}; skills: {skills}; experience: {yrs} years
                        JOB: {title} at {company}
                        {jd}
                        """)
                        .param("q", question)
                        .param("summary", String.valueOf(u.getProfileSummary()))
                        .param("skills", String.valueOf(u.getSkills()))
                        .param("yrs", String.valueOf(u.getTotalExperienceYears()))
                        .param("title", String.valueOf(job.getTitle()))
                        .param("company", String.valueOf(job.getCompany()))
                        .param("jd", jd))
                .call().content();
        return a == null || a.trim().equalsIgnoreCase("UNKNOWN") ? null : a.trim();
    }
}