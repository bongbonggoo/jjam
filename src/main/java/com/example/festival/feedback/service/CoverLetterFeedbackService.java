package com.example.festival.feedback.service;

import com.example.festival.coverletter.entity.CoverLetter;
import com.example.festival.coverletter.repository.CoverLetterRepository;
import com.example.festival.feedback.dto.FeedbackRequest;
import com.example.festival.feedback.dto.FeedbackResponse;
import com.example.festival.feedback.dto.GenerateFormRequest;
import com.example.festival.feedback.dto.GenerateFormResponse;
import com.example.festival.feedback.entity.Feedback;
import com.example.festival.feedback.entity.Question;
import com.example.festival.feedback.repository.FeedbackRepository;
import com.example.festival.feedback.repository.QuestionRepository;
import com.example.festival.global.ai.OpenAiClient;
import com.example.festival.user.entity.User;
import com.example.festival.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoverLetterFeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final UserRepository userRepository;
    private final OpenAiClient openAiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoverLetterFeedbackService(
            FeedbackRepository feedbackRepository,
            QuestionRepository questionRepository,
            CoverLetterRepository coverLetterRepository,
            UserRepository userRepository,
            OpenAiClient openAiClient
    ) {
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.userRepository = userRepository;
        this.openAiClient = openAiClient;
    }

    // ===============================
    // 1) 양식 생성
    // ===============================
    @Transactional
    public GenerateFormResponse generateForm(Long userId, GenerateFormRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        CoverLetter coverLetter = new CoverLetter();
        coverLetter.setTitle(request.getCompanyStyle() + " - " + request.getJobPosition());
        coverLetter.setStyle(request.getCompanyStyle());
        coverLetter.setStatus("작성중");
        coverLetter.setCreatedAt(LocalDateTime.now());
        coverLetterRepository.save(coverLetter);

        List<Question> questions = createQuestionsFromAi(
                request.getCompanyStyle(),
                request.getJobPosition(),
                request.getInterviewerStyle(),
                coverLetter
        );

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setQuestionNumber(i + 1);
            questionRepository.save(q);
        }

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

    // ===============================
    // 2) 답변 제출 + 피드백 생성
    // ===============================
    @Transactional
    public FeedbackResponse submitAndGetFeedback(Long userId, FeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        CoverLetter coverLetter = coverLetterRepository.findById(request.getCoverLetterId())
                .orElseThrow(() -> new RuntimeException("자소서를 찾을 수 없습니다"));

        List<Question> questions =
                questionRepository.findByCoverLetterIdOrderByQuestionNumber(coverLetter.getId());

        StringBuilder qaText = new StringBuilder();
        qaText.append("기업 스타일: ").append(ns(coverLetter.getStyle())).append("\n");
        qaText.append("지원 직무/포지션: ").append(ns(coverLetter.getTitle())).append("\n\n");

        for (Question question : questions) {
            String answer = request.getAnswers().get(question.getId());
            question.setAnswer(answer);
            questionRepository.save(question);

            qaText.append("Q").append(question.getQuestionNumber()).append(". ")
                    .append(ns(question.getQuestionText())).append("\n");
            qaText.append("A").append(question.getQuestionNumber()).append(". ")
                    .append(ns(answer)).append("\n\n");
        }

        Feedback feedback = createFeedbackFromAi(user, coverLetter, qaText.toString());
        feedbackRepository.save(feedback);

        coverLetter.setStatus("피드백 완료");
        coverLetterRepository.save(coverLetter);

        return convertToResponse(feedback);
    }

    // ===============================
    // 3) 조회
    // ===============================
    public List<FeedbackResponse> getUserFeedbacks(Long userId) {
        List<Feedback> feedbacks = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return feedbacks.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public FeedbackResponse getFeedbackById(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("피드백을 찾을 수 없습니다"));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }
        return convertToResponse(feedback);
    }

    // =========================================================
    // AI: 질문 생성 (배열/객체 둘 다 처리)
    // =========================================================
    private List<Question> createQuestionsFromAi(
            String companyStyle,
            String jobPosition,
            String interviewerStyle,
            CoverLetter coverLetter
    ) {
        String system = "너는 채용 담당자이자 자기소개서 질문 설계 전문가다. 출력은 반드시 순수 JSON만 반환한다.";

        String user = String.format(
                "기업 스타일: %s\n직무: %s\n면접관 스타일: %s\n\n" +
                        "반드시 아래 둘 중 하나 형식으로만 JSON 반환:\n" +
                        "1) JSON 배열:\n" +
                        "[{\"questionText\":\"...\",\"guideline\":\"...\"}]\n\n" +
                        "2) JSON 객체:\n" +
                        "{\"questions\":[{\"questionText\":\"...\",\"guideline\":\"...\"}]}\n",
                ns(companyStyle), ns(jobPosition), ns(interviewerStyle)
        );

        String raw = openAiClient.generateText(system, user, 700);

        try {
            String json = cleanupJson(raw);

            // 1) 배열
            if (json.startsWith("[")) {
                List<Map<String, String>> parsed = objectMapper.readValue(
                        json, new TypeReference<List<Map<String, String>>>() {}
                );
                return toQuestions(parsed, coverLetter);
            }

            // 2) 객체 { "questions": [...] }
            JsonNode node = objectMapper.readTree(json);
            JsonNode qNode = node.get("questions");
            if (qNode == null || !qNode.isArray()) {
                throw new RuntimeException("AI JSON에 questions 배열이 없습니다.");
            }

            List<Map<String, String>> parsed = objectMapper.convertValue(
                    qNode, new TypeReference<List<Map<String, String>>>() {}
            );
            return toQuestions(parsed, coverLetter);

        } catch (Exception e) {
            throw new RuntimeException("AI 질문 생성 실패: " + e.getMessage() + " / raw=" + raw, e);
        }
    }

    private List<Question> toQuestions(List<Map<String, String>> parsed, CoverLetter coverLetter) {
        if (parsed == null || parsed.isEmpty()) {
            throw new RuntimeException("AI가 질문을 비워서 반환했습니다.");
        }

        List<Question> out = new ArrayList<>();
        for (Map<String, String> item : parsed) {
            String qt = item.get("questionText");
            String gl = item.get("guideline");
            if (qt == null || qt.isBlank()) continue;

            Question q = new Question();
            q.setCoverLetter(coverLetter);
            q.setQuestionText(qt.trim());
            q.setGuideline(gl == null ? "" : gl.trim());
            out.add(q);
        }

        if (out.isEmpty()) throw new RuntimeException("AI 질문 파싱 결과가 비었습니다.");
        return out;
    }

    // =========================================================
    // AI: 피드백 생성 (객체 형태 고정)
    // =========================================================
    private Feedback createFeedbackFromAi(User user, CoverLetter coverLetter, String qaText) {
        String system = "너는 자기소개서 첨삭 전문가다. 출력은 반드시 순수 JSON만. 점수는 0~100 정수.";

        String userPrompt =
                "아래 Q/A를 평가해 JSON으로 반환해라.\n\n" +
                        "출력 JSON 형식:\n" +
                        "{\n" +
                        "  \"overallScore\": 0,\n" +
                        "  \"logicScore\": 0,\n" +
                        "  \"persuasivenessScore\": 0,\n" +
                        "  \"consistencyScore\": 0,\n" +
                        "  \"sincerityScore\": 0,\n" +
                        "  \"grammarScore\": 0,\n" +
                        "  \"strengths\": \"...\",\n" +
                        "  \"improvements\": \"...\",\n" +
                        "  \"summary\": \"...\"\n" +
                        "}\n\n" +
                        "Q/A:\n" + qaText;

        String raw = openAiClient.generateText(system, userPrompt, 1100);

        try {
            String json = cleanupJson(raw);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            Feedback feedback = new Feedback();
            feedback.setUser(user);
            feedback.setCoverLetter(coverLetter);

            feedback.setOverallScore(asInt(map.get("overallScore")));
            feedback.setLogicScore(asInt(map.get("logicScore")));
            feedback.setPersuasivenessScore(asInt(map.get("persuasivenessScore")));
            feedback.setConsistencyScore(asInt(map.get("consistencyScore")));
            feedback.setSincerityScore(asInt(map.get("sincerityScore")));
            feedback.setGrammarScore(asInt(map.get("grammarScore")));

            feedback.setStrengths(String.valueOf(map.getOrDefault("strengths", "")));
            feedback.setImprovements(String.valueOf(map.getOrDefault("improvements", "")));
            feedback.setSummary(String.valueOf(map.getOrDefault("summary", "")));

            return feedback;

        } catch (Exception e) {
            throw new RuntimeException("AI 피드백 생성 실패: " + e.getMessage() + " / raw=" + raw, e);
        }
    }

    // =========================================================
    // Utils
    // =========================================================
    private String ns(String s) {
        return s == null ? "" : s;
    }

    private int asInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(String.valueOf(v).trim());
    }

    private String cleanupJson(String s) {
        if (s == null) return "";
        String out = s.trim();
        if (out.startsWith("```")) out = out.replaceFirst("^```(json)?", "").trim();
        if (out.endsWith("```")) out = out.substring(0, out.length() - 3).trim();
        return out.trim();
    }

    private FeedbackResponse convertToResponse(Feedback feedback) {
        Map<String, Integer> scores = new HashMap<String, Integer>();
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
