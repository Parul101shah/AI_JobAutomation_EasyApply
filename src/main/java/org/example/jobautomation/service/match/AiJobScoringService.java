package org.example.jobautomation.service.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.AiScoreResultDto;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.entity.UserProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobScoringService {

    private static final int MAX_DESCRIPTION_CHARS = 3000;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final JobTextNormalizer jobTextNormalizer;

    public AiScoreResultDto score(UserProfile profile, JobListingDto job) {
        String prompt = buildPrompt(profile, job);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return fallback("Empty AI response");
            }

            response = response
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            AiScoreResultDto dto = objectMapper.readValue(response, AiScoreResultDto.class);

            if (dto.getScore() == null) {
                dto.setScore(0);
            }

            dto.setScore(Math.max(0, Math.min(100, dto.getScore())));

            if (dto.getMatchLevel() == null || dto.getMatchLevel().isBlank()) {
                dto.setMatchLevel("UNKNOWN");
            }

            return dto;
        } catch (Exception ex) {
            log.warn("AI job scoring failed for job '{}' : {}", job.getTitle(), ex.toString());
            return fallback("AI scoring unavailable");
        }
    }

    private AiScoreResultDto fallback(String summary) {
        AiScoreResultDto dto = new AiScoreResultDto();
        dto.setScore(0);
        dto.setMatchLevel("UNKNOWN");
        dto.setSummary(summary);
        return dto;
    }

    private String buildPrompt(UserProfile profile, JobListingDto job) {
        return """
                You are evaluating whether a candidate is a good fit for a job.

                Return ONLY valid JSON. Do not include markdown or extra text.

                Use exactly this schema:
                {
                  "score": <integer 0-100>,
                  "matchLevel": "HIGH|MEDIUM|LOW|REJECT",
                  "summary": "short 1-2 line summary",
                  "reasons": ["reason 1", "reason 2"],
                  "concerns": ["concern 1", "concern 2"]
                }

                Rules:
                - Score must be an integer from 0 to 100.
                - Be strict about clear mismatches.
                - Consider role alignment, skills overlap, experience fit, and location compatibility.
                - Do not invent facts.
                - Keep reasons and concerns concise.

                CANDIDATE PROFILE
                Full Name: %s
                Target Roles: %s
                Skills: %s
                Past Roles: %s
                Total Experience Years: %s
                Preferred Location: %s
                Education: %s
                Profile Summary: %s

                JOB DETAILS
                Title: %s
                Company: %s
                Location: %s
                Description:
                %s
                """.formatted(
                safe(profile.getFullName()),
                safe(profile.getTargetRoles()),
                safe(profile.getSkills()),
                safe(profile.getPastRoles()),
                safe(profile.getTotalExperienceYears()),
                safe(profile.getPreferredLocation()),
                safe(profile.getEducation()),
                safe(profile.getProfileSummary()),
                safe(job.getTitle()),
                safe(job.getCompany()),
                safe(job.getLocation()),
                truncate(jobTextNormalizer.toPlainText(job.getDescription()), MAX_DESCRIPTION_CHARS)
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}