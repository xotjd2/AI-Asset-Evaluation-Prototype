package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@Table(name = "evaluation_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EvaluationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String evaluationType;

    @Column(nullable = false)
    private String targetName;

    @Column(precision = 18, scale = 4)
    private BigDecimal presentValue;

    @Column(length = 50)
    private String riskMetricName;

    @Column(precision = 18, scale = 4)
    private BigDecimal riskMetricValue;

    @Column(precision = 18, scale = 4)
    private BigDecimal durationValue;

    @Column(precision = 18, scale = 4)
    private BigDecimal convexity;

    @Column(precision = 18, scale = 4)
    private BigDecimal npv;

    @Column(precision = 18, scale = 4)
    private BigDecimal irr;

    @Column(precision = 18, scale = 4)
    private BigDecimal paybackPeriod;

    @Column(nullable = false, length = 20)
    private String riskGrade;

    @Column(nullable = false, length = 1000)
    private String modelExplanation;

    @Column(nullable = false, length = 500)
    private String commentary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
