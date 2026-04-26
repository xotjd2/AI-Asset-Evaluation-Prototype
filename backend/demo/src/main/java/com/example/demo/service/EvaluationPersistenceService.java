package com.example.demo.service;

import com.example.demo.domain.EvaluationRecord;
import com.example.demo.dto.EvaluationSummaryResponse;
import com.example.demo.dto.SaveEvaluationRequest;
import com.example.demo.repository.EvaluationRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EvaluationPersistenceService {

    private static final Set<String> VALID_TYPES = Set.of("주식", "채권", "프로젝트", "stock", "bond", "project");
    private static final Set<String> VALID_GRADES = Set.of("안전", "보통", "주의", "위험");

    private final EvaluationRecordRepository repository;

    public EvaluationPersistenceService(EvaluationRecordRepository repository) {
        this.repository = repository;
    }

    public EvaluationSummaryResponse save(SaveEvaluationRequest response) {
        validateSaveRequest(response);

        EvaluationRecord record = EvaluationRecord.builder()
                .evaluationType(response.evaluationType())
                .targetName(response.targetName())
                .presentValue(decimal(response.presentValue()))
                .riskMetricName(response.riskMetricName())
                .riskMetricValue(decimal(response.riskMetricValue()))
                .durationValue(decimal(response.duration()))
                .convexity(decimal(response.convexity()))
                .npv(decimal(response.npv()))
                .irr(decimal(response.irr()))
                .paybackPeriod(decimal(response.paybackPeriod()))
                .riskGrade(response.riskGrade())
                .modelExplanation(response.modelExplanation())
                .commentary(response.commentary())
                .build();
        EvaluationRecord saved = repository.save(record);

        return new EvaluationSummaryResponse(
                saved.getEvaluationType(),
                saved.getTargetName(),
                toDouble(saved.getPresentValue()),
                saved.getRiskMetricName(),
                toDouble(saved.getRiskMetricValue()),
                toDouble(saved.getDurationValue()),
                toDouble(saved.getConvexity()),
                toDouble(saved.getNpv()),
                toDouble(saved.getIrr()),
                toDouble(saved.getPaybackPeriod()),
                saved.getRiskGrade(),
                response.riskScore(),
                saved.getModelExplanation(),
                saved.getCommentary(),
                response.highlights(),
                saved.getCreatedAt() == null ? LocalDateTime.now() : saved.getCreatedAt()
        );
    }

    public List<EvaluationSummaryResponse> getRecent() {
        return repository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(record -> new EvaluationSummaryResponse(
                        record.getEvaluationType(),
                        record.getTargetName(),
                        toDouble(record.getPresentValue()),
                        record.getRiskMetricName(),
                        toDouble(record.getRiskMetricValue()),
                        toDouble(record.getDurationValue()),
                        toDouble(record.getConvexity()),
                        toDouble(record.getNpv()),
                        toDouble(record.getIrr()),
                        toDouble(record.getPaybackPeriod()),
                        record.getRiskGrade(),
                        0.0,
                        record.getModelExplanation(),
                        record.getCommentary(),
                        List.of("저장된 평가 이력입니다."),
                        record.getCreatedAt()
                ))
                .toList();
    }

    private void validateSaveRequest(SaveEvaluationRequest request) {
        String type = request.evaluationType();
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("지원하지 않는 평가 유형입니다.");
        }
        if (!VALID_GRADES.contains(request.riskGrade())) {
            throw new IllegalArgumentException("위험등급 값이 올바르지 않습니다.");
        }

        validateFiniteAndRange(request.presentValue(), "평가금액", 0.0, Double.MAX_VALUE);
        validateFiniteAndRange(request.riskMetricValue(), "위험지표 값", 0.0, Double.MAX_VALUE);
        validateFiniteAndRange(request.riskScore(), "위험점수", 0.0, 100.0);

        if (request.highlights().isEmpty()) {
            throw new IllegalArgumentException("하이라이트는 최소 1개 이상 필요합니다.");
        }

        if ("주식".equals(type) || "stock".equals(type)) {
            requireZero(request.duration(), "주식 저장값에는 듀레이션이 포함되지 않습니다.");
            requireZero(request.convexity(), "주식 저장값에는 컨벡서티가 포함되지 않습니다.");
            requireZero(request.npv(), "주식 저장값에는 NPV가 포함되지 않습니다.");
            requireZero(request.irr(), "주식 저장값에는 IRR이 포함되지 않습니다.");
            requireZero(request.paybackPeriod(), "주식 저장값에는 회수기간이 포함되지 않습니다.");
        }

        if ("채권".equals(type) || "bond".equals(type)) {
            validateFiniteAndRange(request.duration(), "듀레이션", 0.0, Double.MAX_VALUE);
            validateFiniteAndRange(request.convexity(), "컨벡서티", 0.0, Double.MAX_VALUE);
            requireZero(request.npv(), "채권 저장값에는 NPV가 포함되지 않습니다.");
            requireZero(request.irr(), "채권 저장값에는 IRR이 포함되지 않습니다.");
            requireZero(request.paybackPeriod(), "채권 저장값에는 회수기간이 포함되지 않습니다.");
        }

        if ("프로젝트".equals(type) || "project".equals(type)) {
            validateFinite(request.npv(), "NPV");
            validateFiniteAndRange(request.irr(), "IRR", -1.0, 10.0);
            validateFiniteAndRange(request.paybackPeriod(), "회수기간", 0.0, Double.MAX_VALUE);
            requireZero(request.duration(), "프로젝트 저장값에는 듀레이션이 포함되지 않습니다.");
            requireZero(request.convexity(), "프로젝트 저장값에는 컨벡서티가 포함되지 않습니다.");
        }
    }

    private void validateFiniteAndRange(double value, String fieldName, double min, double max) {
        validateFinite(value, fieldName);
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " 범위를 확인해주세요.");
        }
    }

    private void validateFinite(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + " 값이 올바르지 않습니다.");
        }
    }

    private void requireZero(double value, String message) {
        validateFinite(value, "저장값");
        if (Math.abs(value) > 0.0001) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
