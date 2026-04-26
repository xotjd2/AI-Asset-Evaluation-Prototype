package com.example.demo.service;

import com.example.demo.domain.EvaluationRecord;
import com.example.demo.dto.SaveEvaluationRequest;
import com.example.demo.repository.EvaluationRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationPersistenceServiceTest {

    private final EvaluationRecordRepository repository = mock(EvaluationRecordRepository.class);
    private final EvaluationPersistenceService service = new EvaluationPersistenceService(repository);

    @Test
    void saveAcceptsValidBondResult() {
        SaveEvaluationRequest request = new SaveEvaluationRequest(
                "채권",
                "국고채 3년",
                102000000,
                "금리충격손실",
                1800000,
                2.73,
                8.45,
                0.0,
                0.0,
                0.0,
                "보통",
                61.2,
                "채권 가격은 할인현금흐름 방식으로 계산했습니다.",
                "금리 충격에 대한 손실 부담이 관리 가능한 수준입니다.",
                List.of("평가금액 산출", "금리충격손실 반영")
        );

        when(repository.save(any(EvaluationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.save(request);

        assertEquals("채권", saved.evaluationType());
        assertEquals("국고채 3년", saved.targetName());
        assertEquals(61.2, saved.riskScore());
    }

    @Test
    void saveRejectsOutOfRangeRiskScore() {
        SaveEvaluationRequest request = new SaveEvaluationRequest(
                "주식",
                "삼성전자",
                100000000,
                "VaR 95%",
                12000000,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                "주의",
                120.0,
                "기대수익률과 변동성을 기준으로 계산했습니다.",
                "변동성 부담이 있어 보수적 접근이 필요합니다.",
                List.of("VaR 반영")
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.save(request));

        assertEquals("위험점수 범위를 확인해주세요.", exception.getMessage());
    }
}
