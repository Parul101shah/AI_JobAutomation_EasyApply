package org.example.jobautomation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.jobautomation.dto.JobSourceType;
import java.time.Instant;

@Entity
@Table(name = "generated_resumes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "source", "externalJobId"}))
@Data @NoArgsConstructor
public class GeneratedResume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING) private JobSourceType source;
    private String externalJobId;
    private String jobTitle;
    private String company;
    @Column(columnDefinition = "TEXT") private String tailoredJson;
    @Column(length = 10_000_000) private byte[] pdf;
    private Instant createdAt = Instant.now();
}