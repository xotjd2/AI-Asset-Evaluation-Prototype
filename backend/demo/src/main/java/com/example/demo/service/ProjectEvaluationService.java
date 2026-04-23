package com.example.demo.service;

import com.example.demo.dto.EvaluationSummaryResponse;
import com.example.demo.dto.ProjectEvaluationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectEvaluationService {

    private static final double LOSS_GIVEN_DEFAULT = 0.45;

    private final RiskScoringService riskScoringService;

    public ProjectEvaluationService(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    public EvaluationSummaryResponse evaluate(ProjectEvaluationRequest request) {
        validate(request);

        double npv = -request.initialInvestment();
        double cumulative = -request.initialInvestment();
        double paybackPeriod = request.cashFlows().size() + 1.0;

        for (int year = 0; year < request.cashFlows().size(); year++) {
            double cashFlow = request.cashFlows().get(year);
            npv += cashFlow / Math.pow(1 + request.discountRate(), year + 1);
            cumulative += cashFlow;
            if (cumulative >= 0 && paybackPeriod > request.cashFlows().size()) {
                paybackPeriod = year + 1;
            }
        }

        double irr = estimateIrr(request.initialInvestment(), request.cashFlows(), request.discountRate());
        double presentValue = npv + request.initialInvestment();
        double creditRiskEstimate = Math.max(presentValue, 0.0) * request.probabilityOfDefault() * LOSS_GIVEN_DEFAULT;
        double profitabilityRatio = npv / Math.max(request.initialInvestment(), 1.0);
        double paybackPenalty = Math.max(paybackPeriod - 3, 0) * 8;
        double defaultPenalty = request.probabilityOfDefault() * 120;
        double creditPenalty = (creditRiskEstimate / Math.max(request.initialInvestment(), 1.0)) * 100;
        double irrContribution = Math.max(Math.min(irr * 100, 18), -20);
        double profitabilityContribution = Math.max(Math.min(profitabilityRatio * 120, 20), -35);
        double baseScore = 60.0;

        double score = riskScoringService.clampScore(
                baseScore
                        + profitabilityContribution * 0.8
                        + irrContribution * 0.7
                        - defaultPenalty * 0.8
                        - paybackPenalty * 0.7
                        - creditPenalty * 0.9
        );
        String grade = riskScoringService.grade(score);

        return new EvaluationSummaryResponse(
                "프로젝트",
                request.projectName(),
                round(presentValue),
                "신용위험추정치",
                round(creditRiskEstimate),
                0.0,
                0.0,
                round(npv),
                round(irr),
                round(paybackPeriod),
                grade,
                round(score),
                "프로젝트 가치는 할인율을 적용한 NPV와 IRR로 계산했고, 위험지표는 현재가치에 부도확률과 손실률을 곱한 단순 신용위험 추정치입니다.",
                grade.equals("위험")
                        ? "부도확률과 자금회수 속도를 고려하면 구조 조정 검토가 필요한 프로젝트입니다."
                        : "현재 가정에서는 현금흐름과 회수기간이 비교적 설명 가능한 프로젝트입니다.",
                List.of(
                        "NPV는 초기 투자비를 포함한 순현재가치입니다.",
                        "IRR은 할인율 근처에서 탐색 범위를 확장해 추정했습니다.",
                        "위험점수는 기준점 60에 수익성, IRR, 부도확률, 회수기간, 신용위험추정치 비중을 가중 반영했습니다."
                ),
                LocalDateTime.now()
        );
    }

    private double estimateIrr(double initialInvestment, List<Double> cashFlows, double discountRate) {
        double low = -0.90;
        double high = Math.max(0.30, discountRate * 4 + 0.20);
        double lowNpv = projectNpv(low, initialInvestment, cashFlows);
        double highNpv = projectNpv(high, initialInvestment, cashFlows);

        int expandCount = 0;
        while (lowNpv * highNpv > 0 && expandCount < 8) {
            high += 0.25;
            highNpv = projectNpv(high, initialInvestment, cashFlows);
            expandCount++;
        }

        if (lowNpv * highNpv > 0) {
            return discountRate;
        }

        for (int i = 0; i < 100; i++) {
            double mid = (low + high) / 2;
            double midNpv = projectNpv(mid, initialInvestment, cashFlows);
            if (midNpv > 0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2;
    }

    private double projectNpv(double rate, double initialInvestment, List<Double> cashFlows) {
        double npv = -initialInvestment;
        for (int year = 0; year < cashFlows.size(); year++) {
            npv += cashFlows.get(year) / Math.pow(1 + rate, year + 1);
        }
        return npv;
    }

    private void validate(ProjectEvaluationRequest request) {
        if (request.initialInvestment() <= 0 || request.discountRate() <= 0 || request.cashFlows() == null || request.cashFlows().isEmpty()) {
            throw new IllegalArgumentException("프로젝트 입력값을 다시 확인해주세요.");
        }
        if (request.probabilityOfDefault() < 0 || request.probabilityOfDefault() > 1) {
            throw new IllegalArgumentException("부도확률은 0 이상 1 이하여야 합니다.");
        }
        if (request.discountRate() > 1) {
            throw new IllegalArgumentException("할인율과 부도확률은 소수로 입력해주세요. 예: 9%는 0.09");
        }
        if (Double.isNaN(request.initialInvestment()) || Double.isNaN(request.discountRate()) || Double.isNaN(request.probabilityOfDefault())) {
            throw new IllegalArgumentException("숫자 입력값을 다시 확인해주세요.");
        }
        boolean hasPositiveCashFlow = false;
        for (Double cashFlow : request.cashFlows()) {
            if (cashFlow == null || Double.isNaN(cashFlow) || Double.isInfinite(cashFlow)) {
                throw new IllegalArgumentException("현금흐름에 잘못된 숫자가 포함되어 있습니다.");
            }
            if (cashFlow > 0) {
                hasPositiveCashFlow = true;
            }
            if (Math.abs(cashFlow) > 1000000000000000.0) {
                throw new IllegalArgumentException("현금흐름 금액 범위를 확인해주세요.");
            }
        }
        if (!hasPositiveCashFlow) {
            throw new IllegalArgumentException("현금흐름에는 최소 1개 이상의 양수 값이 필요합니다.");
        }
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
