package org.example.jobautomation.repository;

import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.entity.GeneratedResume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedResumeRepository extends JpaRepository<GeneratedResume, Long> {
    Optional<GeneratedResume> findByUserIdAndSourceAndExternalJobId(Long userId, JobSourceType source, String jobId);
    List<GeneratedResume> findByUserIdOrderByCreatedAtDesc(Long userId);
}
