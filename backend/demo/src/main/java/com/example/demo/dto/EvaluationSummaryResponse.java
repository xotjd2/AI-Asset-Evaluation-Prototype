package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationSummaryResponse(
        String evaluationType,
        String targetName,
        double presentValue,
        String riskMetricName,
        double riskMetricValue,
        double duration,
        double convexity,
        double npv,
        double irr,
        double paybackPeriod,
        String riskGrade,
        double riskScore,
        String modelExplanation,
        String commentary,
        List<String> highlights,
        LocalDateTime evaluatedAt
) {
}
