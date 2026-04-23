package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveEvaluationRequest(
        @NotBlank(message = "평가 유형은 필수입니다.")
        String evaluationType,

        @NotBlank(message = "평가 대상명은 필수입니다.")
        @Size(max = 100, message = "평가 대상명은 100자 이하여야 합니다.")
        String targetName,
        double presentValue,

        @NotBlank(message = "위험지표명은 필수입니다.")
        String riskMetricName,
        double riskMetricValue,
        double duration,
        double convexity,
        double npv,
        double irr,
        double paybackPeriod,

        @NotBlank(message = "위험등급은 필수입니다.")
        String riskGrade,
        double riskScore,

        @NotBlank(message = "계산 설명은 필수입니다.")
        @Size(max = 1000, message = "계산 설명 길이를 줄여주세요.")
        String modelExplanation,

        @NotBlank(message = "종합 의견은 필수입니다.")
        @Size(max = 500, message = "종합 의견 길이를 줄여주세요.")
        String commentary,

        @NotNull(message = "하이라이트는 필수입니다.")
        List<String> highlights
) {
}
