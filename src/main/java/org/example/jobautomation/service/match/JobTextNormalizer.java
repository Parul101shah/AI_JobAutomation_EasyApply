package org.example.jobautomation.service.match;

import org.springframework.stereotype.Component;

@Component
public class JobTextNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    public String normalizeAndStripHtml(String value) {
        String normalized = normalize(value);
        normalized = normalized
                .replaceAll("&lt;[^&]*&gt;", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&#39;", "'")
                .replaceAll("&amp;", "&");
        return normalized.replaceAll("\\s+", " ").trim();
    }
}