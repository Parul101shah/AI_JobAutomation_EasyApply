package org.example.jobautomation.strategy;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.SelectOption;
import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.service.apply.AnswerResolver;
import org.example.jobautomation.service.apply.AnswerResolver.Kind;
import org.example.jobautomation.service.apply.ApplyContext;
import org.example.jobautomation.service.apply.ApplyResult;
import org.example.jobautomation.service.apply.ApplyResult.Outcome;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeverApplyStrategy implements ApplyStrategy {

    private final AnswerResolver resolver;

    @Override
    public JobSourceType supports() {
        return JobSourceType.LEVER;
    }

    @Override
    public ApplyResult apply(Page page, ApplyContext ctx) {
        String url = ctx.job().getJobUrl();
        if (!url.endsWith("/apply")) {
            url = url.replaceAll("/+$", "") + "/apply";
        }
        page.navigate(url);
        page.waitForLoadState();

        if (page.locator("input[name=name]").count() == 0) {
            return ApplyResult.of(Outcome.NEEDS_MANUAL, "Application form not found.");
        }
        if (page.locator("iframe[src*='hcaptcha'], .h-captcha, iframe[src*='recaptcha'], .g-recaptcha").count() > 0) {
            return ApplyResult.of(Outcome.NEEDS_MANUAL, "CAPTCHA present.");
        }

        // ---- Basic fields ----
        page.fill("input[name=name]", ctx.user().getFullName() == null ? "" : ctx.user().getFullName());
        page.fill("input[name=email]", ctx.user().getEmail());

        String phone = ctx.user().getPhone();
        if (phone != null && !phone.isBlank() && page.locator("input[name=phone]").count() > 0) {
            page.fill("input[name=phone]", phone);
        }
        String linkedin = ctx.user().getLinkedinUrl();
        if (linkedin != null && !linkedin.isBlank() && page.locator("input[name='urls[LinkedIn]']").count() > 0) {
            page.fill("input[name='urls[LinkedIn]']", linkedin);
        }

        // ---- Resume ----
        page.locator("input[name=resume]").setInputFiles(
                new FilePayload("resume.pdf", "application/pdf", ctx.resumePdf()));

        // ---- Custom questions ----
        Locator questions = page.locator("li.application-question");
        for (int i = 0; i < questions.count(); i++) {
            Locator q = questions.nth(i);

            Locator labelLoc = q.locator(".application-label").first();
            if (labelLoc.count() == 0) continue;

            String rawLabel = labelLoc.innerText();
            boolean required = q.locator(".required").count() > 0 || rawLabel.contains("✱") || rawLabel.contains("*");
            if (!required) continue;

            String label = rawLabel.replace("✱", "").replace("*", "").trim();

            Locator textInput = q.locator(
                    "input:not([type=file]):not([type=hidden]):not([type=radio]):not([type=checkbox]), select, textarea").first();
            Locator radios = q.locator("input[type=radio]");
            Locator checks = q.locator("input[type=checkbox]");

            // Skip fields already handled above (resume, name, email, phone, org, urls)
            if (textInput.count() > 0) {
                String name = textInput.getAttribute("name");
                if (name != null && (name.equals("name") || name.equals("email") || name.equals("phone")
                        || name.equals("org") || name.startsWith("urls[") || name.equals("resume"))) {
                    continue;
                }
            }

            // --- Radio / checkbox groups (e.g. Yes / No) ---
            if (radios.count() > 0 || (textInput.count() == 0 && checks.count() > 0)) {
                Locator group = radios.count() > 0 ? radios : checks;
                if (q.locator("input:checked").count() > 0) continue;

                List<String> options = q.locator(".application-answer-alternative, ul li label").allInnerTexts()
                        .stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (options.isEmpty()) options = q.locator("label").allInnerTexts().stream()
                        .map(String::trim).filter(s -> !s.isEmpty() && !s.equals(rawLabel.trim())).toList();

                Optional<String> ans = resolver.resolve(label, Kind.SELECT, options, ctx.user(), ctx.job());
                if (ans.isEmpty()) {
                    return ApplyResult.of(Outcome.NEEDS_MANUAL, "Unanswered required question: " + label);
                }
                Locator target = q.locator("label").filter(new Locator.FilterOptions().setHasText(ans.get())).last();
                if (target.count() == 0) {
                    return ApplyResult.of(Outcome.NEEDS_MANUAL, "Option '" + ans.get() + "' not found for: " + label);
                }
                target.click();
                continue;
            }

            if (textInput.count() == 0 || !textInput.isVisible()) continue;

            // --- Text / textarea / select ---
            String tag = (String) textInput.evaluate("e => e.tagName.toLowerCase()");
            Kind kind = tag.equals("select") ? Kind.SELECT
                    : tag.equals("textarea") ? Kind.TEXTAREA : Kind.TEXT;

            if (kind != Kind.SELECT && !textInput.inputValue().isBlank()) continue; // already filled

            List<String> options = kind == Kind.SELECT
                    ? textInput.locator("option").allInnerTexts().stream()
                    .map(String::trim).filter(s -> !s.isEmpty() && !s.toLowerCase().startsWith("select")).toList()
                    : List.of();

            Optional<String> ans = resolver.resolve(label, kind, options, ctx.user(), ctx.job());
            if (ans.isEmpty()) {
                return ApplyResult.of(Outcome.NEEDS_MANUAL, "Unanswered required question: " + label);
            }

            if (kind == Kind.SELECT) {
                textInput.selectOption(new SelectOption().setLabel(ans.get()));
            } else {
                textInput.fill(ans.get());
            }
        }

        if (ctx.dryRun()) {
            return ApplyResult.of(Outcome.DRY_RUN_OK, "Form filled; dry-run, not submitted.");
        }

        // ---- Submit ----
        page.locator("button[type=submit], #btn-submit").first().click();
        page.waitForLoadState();

        boolean ok = page.url().contains("/thanks")
                || page.textContent("body").toLowerCase().contains("application submitted");
        return ok ? ApplyResult.of(Outcome.SUBMITTED, "Submitted.")
                : ApplyResult.of(Outcome.NEEDS_MANUAL, "Confirmation not detected.");
    }
}