package org.example.jobautomation.service.apply;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.entity.GeneratedResume;
import org.example.jobautomation.entity.ResumeConfirmation;
import org.example.jobautomation.entity.ResumeConfirmationStatus;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.repository.GeneratedResumeRepository;
import org.example.jobautomation.repository.ResumeConfirmationRepository;
import org.example.jobautomation.service.UserProfileService;
import org.example.jobautomation.service.notification.MatchEmailService;
import org.example.jobautomation.service.resume.ResumeGenerationService;
import org.example.jobautomation.strategy.ApplyStrategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplyService {

    private final ResumeConfirmationRepository confirmationRepo;
    private final GeneratedResumeRepository resumeRepo;
    private final UserProfileService userProfileService;
    private final MatchEmailService emailService;
    private final ResumeGenerationService generationService;
    private final PlaywrightManager playwrightManager;
    private final ObjectMapper objectMapper;
    private final ExecutorService applyExecutor;
    private final Map<JobSourceType, ApplyStrategy> strategies;

    @Value("${app.apply.dry-run:true}")
    private boolean dryRun;

    @Value("${app.apply.screenshot-dir:./apply-screenshots}")
    private String screenshotDir;

    public ApplyService(
            ResumeConfirmationRepository confirmationRepo,
            GeneratedResumeRepository resumeRepo,
            UserProfileService userProfileService,
            MatchEmailService emailService,
            ResumeGenerationService generationService,
            PlaywrightManager playwrightManager,
            ObjectMapper objectMapper,
            ExecutorService applyExecutor,
            List<ApplyStrategy> list) {

        this.confirmationRepo = confirmationRepo;
        this.resumeRepo = resumeRepo;
        this.userProfileService = userProfileService;
        this.emailService = emailService;
        this.generationService = generationService;
        this.playwrightManager = playwrightManager;
        this.objectMapper = objectMapper;
        this.applyExecutor = applyExecutor;

        this.strategies = list.stream()
                .collect(Collectors.toMap(
                        ApplyStrategy::supports,
                        Function.identity()
                ));
    }

    /**
     * Handles a user's request to apply through a confirmation token.
     */
    public String requestApply(String token) {

        ResumeConfirmation c = confirmationRepo
                .findByToken(token)
                .orElse(null);

        if (c == null) {
            return "Invalid link.";
        }

        if (c.getExpiresAt() == null
                || c.getExpiresAt().isBefore(Instant.now())) {
            return "This link has expired.";
        }

        if (c.isApplyRequested()) {
            return "An application was already requested ("
                    + c.getStatus() + ").";
        }

        c.setApplyRequested(true);
        confirmationRepo.save(c);

        applyExecutor.submit(() -> run(c.getId()));

        return "Thanks! We're applying to "
                + c.getJobTitle()
                + " at "
                + c.getCompany()
                + ". You'll get an email with the result.";
    }

    /**
     * Performs the application workflow asynchronously.
     */
    void run(Long id) {

        ResumeConfirmation c = confirmationRepo
                .findById(id)
                .orElseThrow();

        JobListingDto job = null;
        UserProfile user;

        try {
            user = userProfileService.getProfile(c.getUserId());

            job = objectMapper.readValue(
                    c.getJobSnapshotJson(),
                    JobListingDto.class
            );

            // Find an existing tailored resume.
            GeneratedResume g = resumeRepo
                    .findByUserIdAndSourceAndExternalJobId(
                            c.getUserId(),
                            c.getSource(),
                            c.getExternalJobId()
                    )
                    .orElse(null);

            // Generate a resume if one does not exist.
            if (g == null) {

                if (c.getStatus() == ResumeConfirmationStatus.PENDING) {
                    c.setStatus(ResumeConfirmationStatus.CONFIRMED);
                    confirmationRepo.save(c);
                }

                generationService.generate(c.getId());

                g = resumeRepo
                        .findByUserIdAndSourceAndExternalJobId(
                                c.getUserId(),
                                c.getSource(),
                                c.getExternalJobId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Resume generation finished, "
                                                + "but no generated resume was found."
                                )
                        );
            }

            // Mark the application as in progress.
            c.setStatus(ResumeConfirmationStatus.APPLYING);
            confirmationRepo.save(c);

            ApplyStrategy strategy = strategies.get(c.getSource());

            ApplyResult result;

            if (strategy == null) {

                result = ApplyResult.of(
                        ApplyResult.Outcome.NEEDS_MANUAL,
                        "No automation for " + c.getSource()
                );

            } else {

                ApplyContext context = new ApplyContext(
                        user,
                        job,
                        g.getPdf(),
                        dryRun
                );

                result = execute(strategy, context, id);
            }

            // Update the final application status.
            c.setStatus(switch (result.outcome()) {
                case SUBMITTED, DRY_RUN_OK ->
                        ResumeConfirmationStatus.APPLIED;

                case NEEDS_MANUAL ->
                        ResumeConfirmationStatus.NEEDS_MANUAL;

                case FAILED ->
                        ResumeConfirmationStatus.FAILED;
            });

            confirmationRepo.save(c);

            emailService.sendApplyResult(
                    user,
                    c.getJobTitle(),
                    c.getCompany(),
                    result.outcome().name(),
                    result.detail(),
                    job.getJobUrl()
            );

        } catch (Exception e) {

            log.error("Apply failed for confirmation {}", id, e);

            c.setStatus(ResumeConfirmationStatus.FAILED);
            confirmationRepo.save(c);

            // Avoid letting an email failure hide the original error.
            try {
                UserProfile profile =
                        userProfileService.getProfile(c.getUserId());

                emailService.sendApplyResult(
                        profile,
                        c.getJobTitle(),
                        c.getCompany(),
                        "FAILED",
                        e.getMessage(),
                        job == null ? "" : job.getJobUrl()
                );

            } catch (Exception emailException) {
                log.error(
                        "Failed to send application failure email for confirmation {}",
                        id,
                        emailException
                );
            }
        }
    }

    /**
     * Executes the source-specific application strategy.
     */
    private ApplyResult execute(
            ApplyStrategy strategy,
            ApplyContext ctx,
            Long id) {

        try (BrowserContext bc = playwrightManager.newContext()) {

            Page page = bc.newPage();
            page.setDefaultTimeout(15_000);

            try {

                ApplyResult result = strategy.apply(page, ctx);

                screenshot(page, id, "result");

                return result;

            } catch (Exception e) {

                screenshot(page, id, "error");

                log.error(
                        "Application strategy failed for confirmation {}",
                        id,
                        e
                );

                return ApplyResult.of(
                        ApplyResult.Outcome.FAILED,
                        e.getClass().getSimpleName()
                                + ": "
                                + e.getMessage()
                );
            }
        }
    }

    /**
     * Saves a screenshot for debugging.
     */
    private void screenshot(Page page, Long id, String tag) {

        try {
            Files.createDirectories(Path.of(screenshotDir));

            page.screenshot(
                    new Page.ScreenshotOptions()
                            .setPath(
                                    Path.of(
                                            screenshotDir,
                                            id + "-" + tag + ".png"
                                    )
                            )
                            .setFullPage(true)
            );

        } catch (Exception e) {
            log.warn(
                    "Could not save {} screenshot for confirmation {}",
                    tag,
                    id,
                    e
            );
        }
    }
}