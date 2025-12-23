package com.example.festival.feedback.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateFormRequest {
    private String companyStyle;
    private String jobPosition;
    private String interviewerStyle;
}