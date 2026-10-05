package org.example.jobautomation.service.match;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExperienceExtractor {

    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)\\s*[-–]\\s*(\\d+)\\+?\\s*years");
    private static final Pattern PLUS_PATTERN = Pattern.compile("(\\d+)\\+\\s*years");
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("(\\d+)\\s*years");

    public Integer extractMinimumYears(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher rangeMatcher = RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            return Integer.parseInt(rangeMatcher.group(1));
        }

        Matcher plusMatcher = PLUS_PATTERN.matcher(text);
        if (plusMatcher.find()) {
            return Integer.parseInt(plusMatcher.group(1));
        }

        Matcher simpleMatcher = SIMPLE_PATTERN.matcher(text);
        if (simpleMatcher.find()) {
            return Integer.parseInt(simpleMatcher.group(1));
        }

        return null;
    }
}
