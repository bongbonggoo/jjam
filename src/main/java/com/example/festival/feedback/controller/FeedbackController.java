package com.example.festival.feedback.controller;

import com.example.festival.feedback.dto.*;
import com.example.festival.feedback.service.FeedbackService;
import com.example.festival.global.security.JwtProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtProvider jwtProvider;

    public FeedbackController(FeedbackService feedbackService, JwtProvider jwtProvider) {
        this.feedbackService = feedbackService;
        this.jwtProvider = jwtProvider;
    }

    // 양식 생성
    @PostMapping("/generate-form")
    public GenerateFormResponse generateForm(
            @RequestHeader("Authorization") String token,
            @RequestBody GenerateFormRequest request) {

        Long userId = getUserIdFromToken(token);
        return feedbackService.generateForm(userId, request);
    }

    // 자소서 제출 및 피드백 받기
    @PostMapping("/submit")
    public FeedbackResponse submitResume(
            @RequestHeader("Authorization") String token,
            @RequestBody FeedbackRequest request) {

        Long userId = getUserIdFromToken(token);
        return feedbackService.submitAndGetFeedback(userId, request);
    }

    // 내 피드백 목록
    @GetMapping("/my")
    public List<FeedbackResponse> getMyFeedbacks(
            @RequestHeader("Authorization") String token) {

        Long userId = getUserIdFromToken(token);
        return feedbackService.getUserFeedbacks(userId);
    }

    // 특정 피드백 조회
    @GetMapping("/{feedbackId}")
    public FeedbackResponse getFeedback(
            @RequestHeader("Authorization") String token,
            @PathVariable Long feedbackId) {

        Long userId = getUserIdFromToken(token);
        return feedbackService.getFeedbackById(userId, feedbackId);
    }

    // 헬퍼 메서드
    private Long getUserIdFromToken(String token) {
        String jwtToken = token.substring(7); // "Bearer " 제거
        return jwtProvider.getUserIdFromToken(jwtToken);
    }
}