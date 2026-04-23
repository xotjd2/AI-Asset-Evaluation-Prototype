package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockEvaluationRequest(
        @NotBlank(message = "종목명은 필수입니다.")
        @Size(max = 100, message = "종목명은 100자 이하여야 합니다.")
        String stockName,

        @DecimalMin(value = "1.0", message = "현재 주가는 1원 이상이어야 합니다.")
        @DecimalMax(value = "1000000000.0", message = "현재 주가 범위를 확인해주세요.")
        double currentPrice,

        @DecimalMin(value = "-0.95", message = "기대 연수익률 범위를 확인해주세요.")
        @DecimalMax(value = "3.0", message = "기대 연수익률 범위를 확인해주세요.")
        double expectedAnnualReturn,

        @DecimalMin(value = "0.0001", message = "변동성은 0보다 커야 합니다.")
        @DecimalMax(value = "3.0", message = "변동성 범위를 확인해주세요.")
        double volatility,

        @Min(value = 1, message = "보유일수는 1일 이상이어야 합니다.")
        @Max(value = 3650, message = "보유일수 범위를 확인해주세요.")
        int holdingDays,

        @Min(value = 1, message = "수량은 1주 이상이어야 합니다.")
        @Max(value = 100000000, message = "수량 범위를 확인해주세요.")
        int shares
) {
}
