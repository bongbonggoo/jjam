package com.example.festival.feedback.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class FeedbackRequest {
    private Long coverLetterId;
    private Map<Long, String> answers; // questionId -> answer
}