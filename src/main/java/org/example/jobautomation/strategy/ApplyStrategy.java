package org.example.jobautomation.strategy;

import com.microsoft.playwright.Page;
import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.service.apply.*;

public interface ApplyStrategy {
    JobSourceType supports();
    ApplyResult apply(Page page, ApplyContext ctx);
}