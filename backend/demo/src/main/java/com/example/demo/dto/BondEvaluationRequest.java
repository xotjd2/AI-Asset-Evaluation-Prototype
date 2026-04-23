package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BondEvaluationRequest(
        @NotBlank(message = "채권명은 필수입니다.")
        @Size(max = 100, message = "채권명은 100자 이하여야 합니다.")
        String bondName,

        @DecimalMin(value = "1.0", message = "액면가는 1원 이상이어야 합니다.")
        @DecimalMax(value = "1000000000000000.0", message = "액면가 범위를 확인해주세요.")
        double faceValue,

        @DecimalMin(value = "0.0", message = "쿠폰금리는 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "쿠폰금리 범위를 확인해주세요.")
        double couponRate,

        @DecimalMin(value = "0.0001", message = "시장수익률은 0보다 커야 합니다.")
        @DecimalMax(value = "1.0", message = "시장수익률 범위를 확인해주세요.")
        double marketYield,

        @Min(value = 1, message = "만기는 1년 이상이어야 합니다.")
        @Max(value = 100, message = "만기 범위를 확인해주세요.")
        int maturityYears,

        @DecimalMin(value = "0.90", message = "신뢰수준 범위를 확인해주세요.")
        @DecimalMax(value = "0.999", message = "신뢰수준 범위를 확인해주세요.")
        double confidenceLevel
) {
}
