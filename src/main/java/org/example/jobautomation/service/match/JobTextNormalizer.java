package org.example.jobautomation.service.match;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class JobTextNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Converts raw or entity-escaped HTML into clean plain text, preserving case.
     * Used for the AI prompt.
     */
    public String toPlainText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        // Pass 1 decodes entities (&lt;h2&gt; -> <h2>); pass 2 strips the real tags.
        String decoded = Jsoup.parse(value).text();
        return Jsoup.parse(decoded).text().replaceAll("\\s+", " ").trim();
    }

    /** Lower-cased plain text. Used by the keyword scorer (signature unchanged). */
    public String normalizeAndStripHtml(String value) {
        return normalize(toPlainText(value));
    }
}