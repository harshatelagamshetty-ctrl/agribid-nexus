package com.agribid.nexus.domain.crop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted result of the Gemini vision grading pipeline.
 * Once the AI's structured output (CropGradeAssessment) is mapped
 * here, it becomes a first-class, foreign-keyed, auditable fact —
 * never left as ephemeral chat text.
 */
@Entity
@Table(name = "quality_grades")
@Getter
@Setter
@NoArgsConstructor
public class QualityGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade_label", nullable = false)
    private String gradeLabel; // e.g. "A", "B", "C"

    @Column(name = "estimated_shelf_life_days")
    private Integer estimatedShelfLifeDays;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "assessed_at", nullable = false)
    private java.time.Instant assessedAt;

    public QualityGrade(String gradeLabel, Integer estimatedShelfLifeDays, Double confidenceScore) {
        this.gradeLabel = gradeLabel;
        this.estimatedShelfLifeDays = estimatedShelfLifeDays;
        this.confidenceScore = confidenceScore;
        this.assessedAt = java.time.Instant.now();
    }
}