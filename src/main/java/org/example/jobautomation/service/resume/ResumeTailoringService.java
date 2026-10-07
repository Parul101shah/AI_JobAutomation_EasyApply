package org.example.jobautomation.service.resume;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.TailoredResume;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.match.JobTextNormalizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeTailoringService {

    private static final int MAX_JD = 4000;
    private final ChatClient.Builder chatClientBuilder;
    private final JobTextNormalizer normalizer;

    public TailoredResume tailor(UserProfile p, JobListingDto job) {
        String rawJd = normalizer.toPlainText(job.getDescription());

        final String jd = rawJd != null && rawJd.length() > MAX_JD
                ? rawJd.substring(0, MAX_JD)
                : rawJd;

        return chatClientBuilder.build().prompt()
                .system("""
                        You are an expert resume writer. Given a job description and a candidate
                        profile, customize the resume to highlight relevant experience.
                        RULES:
                        - Use ONLY facts present in the base resume. Never invent employers,
                          dates, degrees, skills or metrics.
                        - Reorder and reword bullets to emphasize relevance to the job.
                        - Use the job's keywords only where the candidate genuinely has the skill.
                        - Keep it to one page: max 4 bullets per role, summary max 3 lines.
                        - Return only the requested structured JSON.
                        """)
                .user(u -> u.text("""
                                BASE RESUME:
                                {resume}

                                CANDIDATE PROFILE:
                                Name: {name}; Email: {email}; Location: {loc}
                                Skills: {skills}; Experience: {years} years; Education: {edu}

                                JOB: {title} at {company} ({jobLoc})
                                {jd}
                                """)
                        .param("resume", nz(p.getResumeText()))
                        .param("name", nz(p.getFullName()))
                        .param("email", nz(p.getEmail()))
                        .param("loc", nz(p.getPreferredLocation()))
                        .param("skills", String.valueOf(p.getSkills()))
                        .param("years", String.valueOf(p.getTotalExperienceYears()))
                        .param("edu", nz(p.getEducation()))
                        .param("title", nz(job.getTitle()))
                        .param("company", nz(job.getCompany()))
                        .param("jobLoc", nz(job.getLocation()))
                        .param("jd", nz(jd)))
                .call()
                .entity(TailoredResume.class);
    }

    private String nz(String s) { return s == null ? "" : s; }
}

