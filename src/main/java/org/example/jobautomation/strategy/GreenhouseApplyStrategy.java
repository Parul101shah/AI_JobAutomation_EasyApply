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
public class GreenhouseApplyStrategy implements ApplyStrategy {

    private static final List<String> BASIC_IDS = List.of("first_name", "last_name", "email", "phone");

    private final AnswerResolver resolver;

    @Override
    public JobSourceType supports() {
        return JobSourceType.GREENHOUSE;
    }

    @Override
    public ApplyResult apply(Page page, ApplyContext ctx) {
        page.navigate(ctx.job().getJobUrl());
        page.waitForLoadState();

        if (page.locator("#first_name").count() == 0) {
            return ApplyResult.of(Outcome.NEEDS_MANUAL, "Application form not found (embedded or changed layout).");
        }
        if (page.locator("iframe[src*='recaptcha'], .g-recaptcha, iframe[src*='hcaptcha'], .h-captcha").count() > 0) {
            return ApplyResult.of(Outcome.NEEDS_MANUAL, "CAPTCHA present.");
        }

        // ---- Basic fields ----
        page.fill("#first_name", ctx.firstName());
        page.fill("#last_name", ctx.lastName());
        page.fill("#email", ctx.user().getEmail());
        String phone = ctx.user().getPhone();
        if (phone != null && !phone.isBlank() && page.locator("#phone").count() > 0) {
            page.fill("#phone", phone);
        }

        // ---- Resume ----
        page.locator("input[type=file]").first().setInputFiles(
                new FilePayload("resume.pdf", "application/pdf", ctx.resumePdf()));

        // ---- Custom questions ----
        Locator fields = page.locator("#application_form .field, #application-form .field");
        for (int i = 0; i < fields.count(); i++) {
            Locator f = fields.nth(i);

            Locator input = f.locator(
                    "input:not([type=file]):not([type=hidden]):not([type=radio]):not([type=checkbox]), select, textarea").first();
            Locator radios = f.locator("input[type=radio]");

            boolean hasInput = input.count() > 0 && input.isVisible();
            boolean hasRadios = radios.count() > 0;
            if (!hasInput && !hasRadios) continue;

            if (hasInput) {
                String id = input.getAttribute("id");
                if (id != null && BASIC_IDS.contains(id)) continue;
            }

            Locator labelLoc = f.locator("label").first();
            if (labelLoc.count() == 0) continue;
            String rawLabel = labelLoc.innerText();
            boolean required = rawLabel.contains("*")
                    || (hasInput && ("true".equals(input.getAttribute("aria-required"))
                    || input.getAttribute("required") != null));
            if (!required) continue;

            String label = rawLabel.replace("*", "").trim();

            // --- Radio groups ---
            if (!hasInput && hasRadios) {
                if (f.locator("input[type=radio]:checked").count() > 0) continue;
                List<String> options = f.locator("label").allInnerTexts().stream()
                        .map(String::trim).filter(s -> !s.isEmpty() && !s.equals(rawLabel.trim())).toList();
                Optional<String> ans = resolver.resolve(label, Kind.SELECT, options, ctx.user(), ctx.job());
                if (ans.isEmpty()) {
                    return ApplyResult.of(Outcome.NEEDS_MANUAL, "Unanswered required question: " + label);
                }
                f.locator("label").filter(new Locator.FilterOptions().setHasText(ans.get())).last().click();
                continue;
            }

            // --- Text / textarea / select ---
            String tag = ((String) input.evaluate("e => e.tagName.toLowerCase()"));
            Kind kind = tag.equals("select") ? Kind.SELECT
                    : tag.equals("textarea") ? Kind.TEXTAREA : Kind.TEXT;

            if (kind != Kind.SELECT && !input.inputValue().isBlank()) continue; // already filled

            List<String> options = kind == Kind.SELECT
                    ? input.locator("option").allInnerTexts().stream()
                    .map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("--")).toList()
                    : List.of();

            Optional<String> ans = resolver.resolve(label, kind, options, ctx.user(), ctx.job());
            if (ans.isEmpty()) {
                return ApplyResult.of(Outcome.NEEDS_MANUAL, "Unanswered required question: " + label);
            }

            if (kind == Kind.SELECT) {
                input.selectOption(new SelectOption().setLabel(ans.get()));
            } else {
                input.fill(ans.get());
            }
        }

        if (ctx.dryRun()) {
            return ApplyResult.of(Outcome.DRY_RUN_OK, "Form filled; dry-run, not submitted.");
        }

        // ---- Submit ----
        page.locator("button[type=submit], input[type=submit]").first().click();
        page.waitForLoadState();

        String body = page.textContent("body").toLowerCase();
        boolean ok = page.url().contains("confirmation")
                || body.contains("thank you for applying")
                || body.contains("application has been received");
        return ok ? ApplyResult.of(Outcome.SUBMITTED, "Submitted.")
                : ApplyResult.of(Outcome.NEEDS_MANUAL, "Submit clicked but confirmation not detected.");
    }
}
