package com.example.festival.feedback.entity;

import com.example.festival.coverletter.entity.CoverLetter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id", nullable = false)
    private CoverLetter coverLetter;

    private Integer questionNumber;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Column(length = 1000)
    private String guideline;

    @Column(columnDefinition = "TEXT")
    private String answer;
}