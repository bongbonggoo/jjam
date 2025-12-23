package com.example.festival.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class FeedbackResponse {
    private Long feedbackId;
    private Long coverLetterId;
    private String companyStyle;
    private Integer overallScore;
    private Map<String, Integer> scores;
    private String strengths;
    private String improvements;
    private String summary;
    private LocalDateTime createdAt;
}