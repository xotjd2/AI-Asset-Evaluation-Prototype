package com.example.demo.repository;

import com.example.demo.domain.EvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, Long> {
    List<EvaluationRecord> findTop10ByOrderByCreatedAtDesc();
}
