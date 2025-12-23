package com.example.festival.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class GenerateFormResponse {
    private Long coverLetterId;
    private String companyStyle;
    private String jobPosition;
    private List<QuestionDto> questions;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private Integer questionNumber;
        private String question;
        private String guideline;
    }
}