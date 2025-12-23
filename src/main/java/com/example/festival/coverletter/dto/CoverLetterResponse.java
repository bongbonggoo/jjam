package com.example.festival.coverletter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoverLetterResponse {
    private Long id;
    private String title;
    private String content;
    private String status;
    private String style;
}
