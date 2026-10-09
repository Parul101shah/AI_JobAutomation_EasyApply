package org.example.jobautomation.service.apply;

import com.microsoft.playwright.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PlaywrightManager {
    private final Playwright playwright = Playwright.create();
    private final Browser browser;

    public PlaywrightManager(@Value("${app.apply.headless:true}") boolean headless) {
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    public BrowserContext newContext() { return browser.newContext(); }

    @PreDestroy
    void close() { browser.close(); playwright.close(); }
}
