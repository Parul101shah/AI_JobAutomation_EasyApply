package org.example.jobautomation.repository;

import org.example.jobautomation.dto.JobSourceType;
import org.example.jobautomation.entity.ResumeConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface ResumeConfirmationRepository extends JpaRepository<ResumeConfirmation, Long> {
    Optional<ResumeConfirmation> findByToken(String token);
    boolean existsByUserIdAndSourceAndExternalJobId(Long userId, JobSourceType source, String externalJobId);

    @Modifying
    @Transactional
    @Query("""
        update ResumeConfirmation c
           set c.status = org.example.jobautomation.entity.ResumeConfirmation.Status.CONFIRMED
         where c.token = :token
           and c.status = org.example.jobautomation.entity.ResumeConfirmation.Status.PENDING
           and c.expiresAt > :now
        """)
    int claim(@Param("token") String token, @Param("now") Instant now);
}
