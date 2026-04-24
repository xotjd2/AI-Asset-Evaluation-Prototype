package com.example.demo.controller;

import com.example.demo.dto.BondEvaluationRequest;
import com.example.demo.dto.EvaluationSummaryResponse;
import com.example.demo.dto.ProjectEvaluationRequest;
import com.example.demo.dto.SaveEvaluationRequest;
import com.example.demo.dto.StockEvaluationRequest;
import com.example.demo.service.BondEvaluationService;
import com.example.demo.service.EvaluationPersistenceService;
import com.example.demo.service.ProjectEvaluationService;
import com.example.demo.service.StockEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final BondEvaluationService bondEvaluationService;
    private final StockEvaluationService stockEvaluationService;
    private final ProjectEvaluationService projectEvaluationService;
    private final EvaluationPersistenceService persistenceService;

    public EvaluationController(
            BondEvaluationService bondEvaluationService,
            StockEvaluationService stockEvaluationService,
            ProjectEvaluationService projectEvaluationService,
            EvaluationPersistenceService persistenceService
    ) {
        this.bondEvaluationService = bondEvaluationService;
        this.stockEvaluationService = stockEvaluationService;
        this.projectEvaluationService = projectEvaluationService;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/stock")
    public EvaluationSummaryResponse evaluateStock(@Valid @RequestBody StockEvaluationRequest request) {
        return stockEvaluationService.evaluate(request);
    }

    @PostMapping("/bond")
    public EvaluationSummaryResponse evaluateBond(@Valid @RequestBody BondEvaluationRequest request) {
        return bondEvaluationService.evaluate(request);
    }

    @PostMapping("/project")
    public EvaluationSummaryResponse evaluateProject(@Valid @RequestBody ProjectEvaluationRequest request) {
        return projectEvaluationService.evaluate(request);
    }

    @PostMapping("/save")
    public EvaluationSummaryResponse saveEvaluation(@Valid @RequestBody SaveEvaluationRequest request) {
        return persistenceService.save(request);
    }

    @GetMapping("/recent")
    public List<EvaluationSummaryResponse> recentEvaluations() {
        return persistenceService.getRecent();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "입력값을 다시 확인해 주세요." : fieldError.getDefaultMessage();
        return Map.of("message", message);
    }
}
