package org.example.jobautomation.controller;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.service.apply.ApplyService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplyService applyService;

    @GetMapping(value = "/apply/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@PathVariable String token) {
        return """
                <html><body style='font-family:sans-serif;text-align:center;margin-top:60px'>
                <h3>Apply to this job automatically?</h3>
                <form method='post' action='/api/applications/apply/%s'>
                  <button type='submit' style='padding:10px 24px;font-size:16px'>Yes, apply</button>
                </form></body></html>
                """.formatted(HtmlUtils.htmlEscape(token));
    }

    @PostMapping(value = "/apply/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String apply(@PathVariable String token) {
        String msg = applyService.requestApply(token);
        return "<html><body style='font-family:sans-serif;text-align:center;margin-top:60px'><p>"
                + HtmlUtils.htmlEscape(msg) + "</p></body></html>";
    }
}
