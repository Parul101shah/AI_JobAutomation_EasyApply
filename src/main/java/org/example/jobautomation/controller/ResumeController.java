package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.entity.GeneratedResume;
import org.example.jobautomation.repository.GeneratedResumeRepository;
import org.example.jobautomation.service.resume.ResumeGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeGenerationService generationService;
    private final GeneratedResumeRepository resumeRepo;

    // Email link opens this: shows a confirm button (avoids mail scanners auto-triggering the build)
    @GetMapping(value = "/confirm/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String confirmPage(@PathVariable String token) {
        return """
               <html><body style='font-family:sans-serif;text-align:center;margin-top:60px'>
               <h3>Build tailored resume?</h3>
               <form method='post' action='/api/resumes/confirm/%s'>
                 <button type='submit' style='padding:10px 24px;font-size:16px'>Yes, build it</button>
               </form></body></html>
               """.formatted(HtmlUtils.htmlEscape(token));
    }

    @PostMapping(value = "/confirm/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String confirm(@PathVariable String token) {
        String msg = generationService.confirm(token);
        return "<html><body style='font-family:sans-serif;text-align:center;margin-top:60px'><p>"
                + HtmlUtils.htmlEscape(msg) + "</p></body></html>";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        GeneratedResume g = resumeRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-" + id + ".pdf")
                .body(g.getPdf());
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> list(@PathVariable Long userId) {
        return resumeRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(g -> Map.<String, Object>of("id", g.getId(), "source", g.getSource(),
                        "jobId", g.getExternalJobId(), "title", g.getJobTitle(),
                        "company", g.getCompany(), "createdAt", g.getCreatedAt()))
                .toList();
    }
}

