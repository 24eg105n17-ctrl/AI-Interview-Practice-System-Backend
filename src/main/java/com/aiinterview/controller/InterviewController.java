package com.aiinterview.controller;

import com.aiinterview.model.EvaluationResponse;
import com.aiinterview.model.InterviewRequest;
import com.aiinterview.service.AIService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {
    private final AIService aiService;

    public InterviewController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/question")
    public Map<String, String> question(
            @RequestParam String topic,
            @RequestParam(defaultValue = "Intermediate") String difficulty,
            HttpServletRequest request) {
        String[] askedQuestions = request.getParameterValues("askedQuestions");
        List<String> previouslyAsked = askedQuestions == null ? List.of() : Arrays.asList(askedQuestions);
        return aiService.generateQuestion(topic, difficulty, previouslyAsked);
    }

    @PostMapping("/evaluate")
    public EvaluationResponse evaluate(@Valid @RequestBody InterviewRequest request) {
        return aiService.evaluateAnswer(request);
    }
}
