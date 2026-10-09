package org.example.jobautomation.service.apply;

import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.entity.UserProfile;

public record ApplyContext(UserProfile user, JobListingDto job, byte[] resumePdf, boolean dryRun) {
    public String firstName() {
        String n = user.getFullName() == null ? "" : user.getFullName().trim();
        int i = n.indexOf(' ');
        return i < 0 ? n : n.substring(0, i);
    }
    public String lastName() {
        String n = user.getFullName() == null ? "" : user.getFullName().trim();
        int i = n.lastIndexOf(' ');
        return i < 0 ? "" : n.substring(i + 1);
    }
}