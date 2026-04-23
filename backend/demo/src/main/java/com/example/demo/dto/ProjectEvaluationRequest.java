package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectEvaluationRequest(
        @NotBlank(message = "프로젝트명은 필수입니다.")
        @Size(max = 100, message = "프로젝트명은 100자 이하여야 합니다.")
        String projectName,

        @DecimalMin(value = "1.0", message = "초기 투자비는 1원 이상이어야 합니다.")
        @DecimalMax(value = "1000000000000000.0", message = "초기 투자비 범위를 확인해주세요.")
        double initialInvestment,

        @DecimalMin(value = "0.0001", message = "할인율은 0보다 커야 합니다.")
        @DecimalMax(value = "1.0", message = "할인율 범위를 확인해주세요.")
        double discountRate,

        @DecimalMin(value = "0.0", message = "부도확률은 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "부도확률은 1 이하여야 합니다.")
        double probabilityOfDefault,

        @NotEmpty(message = "현금흐름은 최소 1개 이상 필요합니다.")
        @Size(max = 20, message = "현금흐름은 최대 20개까지 입력할 수 있습니다.")
        List<Double> cashFlows
) {
}
