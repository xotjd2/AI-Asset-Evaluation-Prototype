package com.example.demo.service;

import com.example.demo.dto.BondEvaluationRequest;
import com.example.demo.dto.EvaluationSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BondEvaluationService {

    private static final double RATE_SHOCK = 0.0125;

    private final RiskScoringService riskScoringService;

    public BondEvaluationService(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    public EvaluationSummaryResponse evaluate(BondEvaluationRequest request) {
        validate(request);

        double coupon = request.faceValue() * request.couponRate();
        double price = 0.0;
        double weightedDuration = 0.0;
        double convexityNumerator = 0.0;

        for (int t = 1; t <= request.maturityYears(); t++) {
            double cashFlow = t == request.maturityYears() ? coupon + request.faceValue() : coupon;
            double pv = cashFlow / Math.pow(1 + request.marketYield(), t);
            price += pv;
            weightedDuration += t * pv;
            convexityNumerator += cashFlow * t * (t + 1) / Math.pow(1 + request.marketYield(), t + 2);
        }

        double macaulayDuration = weightedDuration / price;
        double modifiedDuration = macaulayDuration / (1 + request.marketYield());
        double convexity = convexityNumerator / price;
        double rateShockLoss = price * (modifiedDuration * RATE_SHOCK - 0.5 * convexity * Math.pow(RATE_SHOCK, 2));
        rateShockLoss = Math.max(rateShockLoss, 0.0);
        double baseScore = 60.0;

        double score = riskScoringService.clampScore(
                baseScore
                        - request.marketYield() * 100 * 0.4
                        - modifiedDuration * 4.5
                        - convexity * 0.6
                        - (rateShockLoss / Math.max(price, 1.0)) * 100 * 0.7
        );
        String grade = riskScoringService.grade(score);

        return new EvaluationSummaryResponse(
                "채권",
                request.bondName(),
                round(price),
                "금리충격손실",
                round(rateShockLoss),
                round(modifiedDuration),
                round(convexity),
                0.0,
                0.0,
                0.0,
                grade,
                round(score),
                "채권 가격은 할인현금흐름 방식으로 계산했고, 금리 민감도는 수정 듀레이션과 컨벡서티로 측정했습니다. 위험지표는 VaR가 아니라 125bp 금리 상승 시의 추정 손실입니다.",
                grade.equals("안전")
                        ? "금리 상승 충격에 대한 손실 추정치가 비교적 낮은 채권입니다."
                        : "금리 상승 충격에 따른 가격 하락 부담이 있어 추가 검토가 필요한 채권입니다.",
                List.of(
                        "평가금액은 쿠폰과 원금을 시장수익률로 할인해 계산했습니다.",
                        "컨벡서티는 각 현금흐름을 추가 할인해 정규화했습니다.",
                        "위험점수는 기준점 60에 수익률, 듀레이션, 컨벡서티, 금리충격손실 비중을 가중 반영했습니다."
                ),
                LocalDateTime.now()
        );
    }

    private void validate(BondEvaluationRequest request) {
        if (request.faceValue() <= 0 || request.couponRate() < 0 || request.marketYield() <= 0 || request.maturityYears() <= 0) {
            throw new IllegalArgumentException("채권 입력값을 다시 확인해주세요.");
        }
        if (request.couponRate() > 1 || request.marketYield() > 1) {
            throw new IllegalArgumentException("금리 값은 소수로 입력해주세요. 예: 4.5%는 0.045");
        }
        if (Double.isNaN(request.faceValue()) || Double.isNaN(request.couponRate()) || Double.isNaN(request.marketYield())) {
            throw new IllegalArgumentException("숫자 입력값을 다시 확인해주세요.");
        }
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
