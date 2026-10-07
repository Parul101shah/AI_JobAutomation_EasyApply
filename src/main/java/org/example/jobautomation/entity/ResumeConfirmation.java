package org.example.jobautomation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.jobautomation.dto.JobSourceType;
import java.time.Instant;

@Entity
@Table(name = "resume_confirmations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "source", "externalJobId"}))
@Data @NoArgsConstructor
public class ResumeConfirmation {
    public enum Status { PENDING, CONFIRMED, COMPLETED, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String token;
    private Long userId;
    @Enumerated(EnumType.STRING) private JobSourceType source;
    private String externalJobId;
    private String jobTitle;
    private String company;
    @Column(columnDefinition = "TEXT") private String jobSnapshotJson;
    private Instant expiresAt;
    @Enumerated(EnumType.STRING) private Status status = Status.PENDING;
}