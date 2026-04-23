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

@Service
public class EvaluationPersistenceService {

    private final EvaluationRecordRepository repository;

    public EvaluationPersistenceService(EvaluationRecordRepository repository) {
        this.repository = repository;
    }

    public EvaluationSummaryResponse save(SaveEvaluationRequest response) {
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

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
