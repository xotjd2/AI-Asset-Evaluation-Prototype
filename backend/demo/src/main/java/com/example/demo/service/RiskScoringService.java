package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    public double clampScore(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }

    public String grade(double score) {
        if (score >= 80) {
            return "안전";
        }
        if (score >= 60) {
            return "보통";
        }
        if (score >= 40) {
            return "주의";
        }
        return "위험";
    }
}
