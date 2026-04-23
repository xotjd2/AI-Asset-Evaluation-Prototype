package com.example.demo.service;

import com.example.demo.dto.EvaluationSummaryResponse;
import com.example.demo.dto.StockEvaluationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockEvaluationService {

    private static final double TRADING_DAYS = 252.0;
    private static final double Z_SCORE_95 = 1.65;

    private final RiskScoringService riskScoringService;

    public StockEvaluationService(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    public EvaluationSummaryResponse evaluate(StockEvaluationRequest request) {
        validate(request);

        double holdingPeriodRatio = request.holdingDays() / TRADING_DAYS;
        double investmentAmount = request.currentPrice() * request.shares();
        double expectedValue = investmentAmount * Math.pow(1 + request.expectedAnnualReturn(), holdingPeriodRatio);
        double parametricVar95 = investmentAmount * request.volatility() * Math.sqrt(holdingPeriodRatio) * Z_SCORE_95;
        double baseScore = 60.0;

        double score = riskScoringService.clampScore(
                baseScore
                        + request.expectedAnnualReturn() * 100 * 0.5
                        - request.volatility() * 100 * 0.7
                        - (parametricVar95 / Math.max(investmentAmount, 1.0)) * 100 * 0.8
        );
        String grade = riskScoringService.grade(score);

        return new EvaluationSummaryResponse(
                "주식",
                request.stockName(),
                round(expectedValue),
                "VaR 95%",
                round(parametricVar95),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                grade,
                round(score),
                "보유 기간 동안의 기대가치는 연환산 기대수익률을 복리로 반영했고, 위험지표는 정규분포 기반 파라메트릭 VaR(95%)로 계산했습니다.",
                grade.equals("안전")
                        ? "기대수익 대비 변동성 부담이 비교적 낮은 주식으로 해석됩니다."
                        : "기대수익 대비 변동성 부담이 있어 보수적인 검토가 필요한 주식입니다.",
                List.of(
                        "기대가치는 보유 일수를 반영한 복리 수익률로 계산했습니다.",
                        "위험지표는 95% 신뢰수준의 파라메트릭 VaR입니다.",
                        "위험점수는 기준점 60에 기대수익률, 변동성, VaR 비중을 가중 반영했습니다."
                ),
                LocalDateTime.now()
        );
    }

    private void validate(StockEvaluationRequest request) {
        if (request.currentPrice() <= 0 || request.volatility() <= 0 || request.holdingDays() <= 0 || request.shares() <= 0) {
            throw new IllegalArgumentException("주식 입력값을 다시 확인해주세요.");
        }
        if (request.expectedAnnualReturn() <= -1.0) {
            throw new IllegalArgumentException("기대 연수익률은 -100%보다 커야 합니다.");
        }
        if (Double.isNaN(request.currentPrice()) || Double.isNaN(request.expectedAnnualReturn()) || Double.isNaN(request.volatility())) {
            throw new IllegalArgumentException("숫자 입력값을 다시 확인해주세요.");
        }
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
