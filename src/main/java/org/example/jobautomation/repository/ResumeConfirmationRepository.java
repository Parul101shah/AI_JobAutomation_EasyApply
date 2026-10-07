package org.example.jobautomation.repository;

import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.entity.ResumeConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeConfirmationRepository extends JpaRepository<ResumeConfirmation, Long> {
    Optional<ResumeConfirmation> findByToken(String token);
    boolean existsByUserIdAndSourceAndExternalJobId(Long userId, JobSourceType source, String externalJobId);
}
