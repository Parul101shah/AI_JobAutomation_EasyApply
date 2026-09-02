package org.example.jobautomation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.ExtractedProfile;
import org.example.jobautomation.dto.UserRegistrationRequest;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.repository.UserProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Handles the 3-step user onboarding:
 *   1. Register (manual preferences)
 *   2. Upload resume → AI extract → return preview
 *   3. Confirm extracted profile (user can edit)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeAIExtractor resumeAIExtractor;


    //Step 1: Register with manual preferences.

    public UserProfile register(UserRegistrationRequest request) {
        UserProfile profile = new UserProfile();
        profile.setFullName(request.getFullName());
        profile.setEmail(request.getEmail());
        profile.setPreferredLocation(request.getPreferredLocation());
        profile.setMinSalary(request.getMinSalary());
        profile.setTargetRoles(request.getTargetRoles());
        return userProfileRepository.save(profile);
    }

    /**
     * Step 2: Upload resume PDF → extract text → AI extract → return preview.
     * Raw text is saved immediately; structured data returned for user review.
     */
    public ExtractedProfile uploadResume(Long userId, MultipartFile file) throws IOException {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Extract raw text from PDF
        String resumeText = resumeTextExtractor.extractText(file);
        log.info("Extracted {} characters from resume for user {}", resumeText.length(), userId);

        // Save raw text
        user.setResumeText(resumeText);
        userProfileRepository.save(user);

        // AI extraction → return preview
        return resumeAIExtractor.extract(resumeText);
    }


    //Step 3: User confirms (or edits) the AI-extracted profile.

    public UserProfile confirmProfile(Long userId, ExtractedProfile extracted) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setSkills(extracted.getSkills());
        user.setPastRoles(extracted.getRoles());
        user.setTotalExperienceYears(extracted.getTotalExperienceYears());
        user.setEducation(extracted.getEducation());
        user.setProfileSummary(extracted.getProfileSummary());

        return userProfileRepository.save(user);
    }

    public UserProfile getProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
}
