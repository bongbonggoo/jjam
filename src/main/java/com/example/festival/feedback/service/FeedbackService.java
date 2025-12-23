package com.example.festival.feedback.service;

import com.example.festival.coverletter.entity.CoverLetter;
import com.example.festival.coverletter.repository.CoverLetterRepository;
import com.example.festival.feedback.dto.*;
import com.example.festival.feedback.entity.Feedback;
import com.example.festival.feedback.entity.Question;
import com.example.festival.feedback.repository.FeedbackRepository;
import com.example.festival.feedback.repository.QuestionRepository;
import com.example.festival.user.entity.User;
import com.example.festival.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           QuestionRepository questionRepository,
                           CoverLetterRepository coverLetterRepository,
                           UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GenerateFormResponse generateForm(Long userId, GenerateFormRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // CoverLetter 생성
        CoverLetter coverLetter = new CoverLetter();
        coverLetter.setTitle(request.getCompanyStyle() + " - " + request.getJobPosition());
        coverLetter.setStyle(request.getCompanyStyle());
        coverLetter.setStatus("작성중");
        coverLetter.setCreatedAt(LocalDateTime.now());
        coverLetterRepository.save(coverLetter);

        // AI가 생성할 질문들 (현재는 더미 데이터)
        List<Question> questions = generateDummyQuestions(coverLetter);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setQuestionNumber(i + 1);
            questionRepository.save(q);
        }

        // Response 생성
        List<GenerateFormResponse.QuestionDto> questionDtos = questions.stream()
                .map(q -> GenerateFormResponse.QuestionDto.builder()
                        .id(q.getId())
                        .questionNumber(q.getQuestionNumber())
                        .question(q.getQuestionText())
                        .guideline(q.getGuideline())
                        .build())
                .collect(Collectors.toList());

        return GenerateFormResponse.builder()
                .coverLetterId(coverLetter.getId())
                .companyStyle(request.getCompanyStyle())
                .jobPosition(request.getJobPosition())
                .questions(questionDtos)
                .build();
    }

    @Transactional
    public FeedbackResponse submitAndGetFeedback(Long userId, FeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        CoverLetter coverLetter = coverLetterRepository.findById(request.getCoverLetterId())
                .orElseThrow(() -> new RuntimeException("자소서를 찾을 수 없습니다"));

        // 답변 저장
        List<Question> questions = questionRepository.findByCoverLetterIdOrderByQuestionNumber(coverLetter.getId());
        for (Question question : questions) {
            String answer = request.getAnswers().get(question.getId());
            question.setAnswer(answer);
            questionRepository.save(question);
        }

        // AI 피드백 생성 (현재는 더미)
        Feedback feedback = generateDummyFeedback(user, coverLetter);
        feedbackRepository.save(feedback);

        coverLetter.setStatus("피드백 완료");
        coverLetterRepository.save(coverLetter);

        return convertToResponse(feedback);
    }

    public List<FeedbackResponse> getUserFeedbacks(Long userId) {
        List<Feedback> feedbacks = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return feedbacks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public FeedbackResponse getFeedbackById(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("피드백을 찾을 수 없습니다"));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        return convertToResponse(feedback);
    }

    // ========== 헬퍼 메서드 ==========

    private List<Question> generateDummyQuestions(CoverLetter coverLetter) {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setCoverLetter(coverLetter);
        q1.setQuestionText("지원 동기와 입사 후 포부를 작성해주세요.");
        q1.setGuideline("기업의 가치와 본인의 목표를 연결하여 작성하세요");
        questions.add(q1);

        Question q2 = new Question();
        q2.setCoverLetter(coverLetter);
        q2.setQuestionText("본인의 강점과 이를 입증할 수 있는 경험을 작성해주세요.");
        q2.setGuideline("구체적인 사례와 결과 중심으로 작성하세요");
        questions.add(q2);

        Question q3 = new Question();
        q3.setCoverLetter(coverLetter);
        q3.setQuestionText("직무 수행을 위한 본인의 역량을 작성해주세요.");
        q3.setGuideline("직무와 관련된 기술과 경험을 강조하세요");
        questions.add(q3);

        return questions;
    }

    private Feedback generateDummyFeedback(User user, CoverLetter coverLetter) {
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setCoverLetter(coverLetter);
        feedback.setOverallScore(85);
        feedback.setLogicScore(80);
        feedback.setPersuasivenessScore(85);
        feedback.setConsistencyScore(90);
        feedback.setSincerityScore(85);
        feedback.setGrammarScore(95);
        feedback.setStrengths("구체적인 경험을 바탕으로 작성되었습니다. 기업의 가치와 본인의 목표가 잘 연결되어 있습니다.");
        feedback.setImprovements("더 구체적인 수치나 성과를 추가하면 좋겠습니다. 문단 간 연결이 더 매끄러우면 좋겠습니다.");
        feedback.setSummary("전반적으로 잘 작성된 자기소개서입니다. 몇 가지 개선사항을 반영하면 더욱 완성도 높은 자소서가 될 것입니다.");
        return feedback;
    }

    private FeedbackResponse convertToResponse(Feedback feedback) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("logic", feedback.getLogicScore());
        scores.put("persuasiveness", feedback.getPersuasivenessScore());
        scores.put("consistency", feedback.getConsistencyScore());
        scores.put("sincerity", feedback.getSincerityScore());
        scores.put("grammar", feedback.getGrammarScore());

        return FeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .coverLetterId(feedback.getCoverLetter().getId())
                .companyStyle(feedback.getCoverLetter().getStyle())
                .overallScore(feedback.getOverallScore())
                .scores(scores)
                .strengths(feedback.getStrengths())
                .improvements(feedback.getImprovements())
                .summary(feedback.getSummary())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}