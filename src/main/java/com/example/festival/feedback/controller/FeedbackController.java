package com.example.festival.feedback.controller;

import com.example.festival.feedback.dto.FeedbackRequest;
import com.example.festival.feedback.dto.FeedbackResponse;
import com.example.festival.feedback.dto.GenerateFormRequest;
import com.example.festival.feedback.dto.GenerateFormResponse;
import com.example.festival.feedback.service.CoverLetterFeedbackService;
import com.example.festival.global.security.JwtProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final CoverLetterFeedbackService feedbackService;
    private final JwtProvider jwtProvider;

    public FeedbackController(
            CoverLetterFeedbackService feedbackService,
            JwtProvider jwtProvider
    ) {
        this.feedbackService = feedbackService;
        this.jwtProvider = jwtProvider;
    }

    // ===============================
    // 1) AI 양식 생성
    // POST /feedback/generate-form
    // ===============================
    @PostMapping("/generate-form")
    public GenerateFormResponse generateForm(
            @RequestHeader("Authorization") String token,
            @RequestBody GenerateFormRequest request
    ) {
        return feedbackService.generateForm(
                getUserIdFromToken(token),
                request
        );
    }

    // ===============================
    // 2) 답변 제출 + AI 피드백 생성
    // POST /feedback/submit
    // ===============================
    @PostMapping("/submit")
    public FeedbackResponse submitResume(
            @RequestHeader("Authorization") String token,
            @RequestBody FeedbackRequest request
    ) {
        return feedbackService.submitAndGetFeedback(
                getUserIdFromToken(token),
                request
        );
    }

    // ===============================
    // 3) 내 피드백 목록 조회
    // GET /feedback/my
    // ===============================
    @GetMapping("/my")
    public List<FeedbackResponse> getMyFeedbacks(
            @RequestHeader("Authorization") String token
    ) {
        return feedbackService.getUserFeedbacks(
                getUserIdFromToken(token)
        );
    }

    // ===============================
    // 4) 피드백 단건 조회
    // GET /feedback/detail/{feedbackId}
    // ===============================
    @GetMapping("/detail/{feedbackId}")
    public FeedbackResponse getFeedback(
            @RequestHeader("Authorization") String token,
            @PathVariable Long feedbackId
    ) {
        return feedbackService.getFeedbackById(
                getUserIdFromToken(token),
                feedbackId
        );
    }

    // ===============================
    // JWT에서 userId 추출
    // ===============================
    private Long getUserIdFromToken(String token) {
        return jwtProvider.getUserIdFromToken(token.substring(7));
    }
}
