package org.example.jobautomation.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.HighMatchDto;
import org.example.jobautomation.entity.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchEmailService {

    private final JavaMailSender mailSender;
    @Value("${app.base-url}") private String baseUrl;
    @Value("${app.mail-from}") private String from;

    public void sendHighMatches(UserProfile user, List<HighMatchDto> jobs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Hi ").append(esc(user.getFullName())).append(",</h3>")
                .append("<p>These jobs are a high match. Click to confirm and we'll build a tailored resume.</p>");
        for (HighMatchDto j : jobs) {
            sb.append("<div style='border:1px solid #ddd;padding:12px;margin:12px 0'>")
                    .append("<b>").append(esc(j.title())).append("</b> – ").append(esc(j.company()))
                    .append("<br>📍 ").append(esc(j.location()))
                    .append("<br>Match <b>").append(j.matchScore()).append("</b> (keyword ")
                    .append(j.keywordScore()).append(", AI ").append(j.aiScore()).append(")")
                    .append("<p>").append(esc(j.aiSummary())).append("</p>")
                    .append("<a href='").append(baseUrl).append("/api/resumes/confirm/").append(j.token())
                    .append("'>✅ Build tailored resume</a> &nbsp; <a href='").append(esc(j.jobUrl()))
                    .append("'>View job</a></div>");
        }
        send(user.getEmail(), "🎯 " + jobs.size() + " high-match jobs – confirm resume build",
                sb.toString(), null, null);
    }

    public void sendResume(UserProfile user, String jobTitle, String company, byte[] pdf) {
        String body = "<p>Hi " + esc(user.getFullName()) + ",</p><p>Your tailored resume for <b>"
                + esc(jobTitle) + "</b> at " + esc(company) + " is attached.</p>";
        send(user.getEmail(), "Your tailored resume – " + jobTitle, body,
                "resume-" + company.replaceAll("[^A-Za-z0-9]", "_") + ".pdf", pdf);
    }

    private void send(String to, String subject, String html, String attName, byte[] att) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, att != null, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            if (att != null) h.addAttachment(attName, new ByteArrayResource(att));
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new IllegalStateException("Email send failed", e);
        }
    }

    private String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
}
