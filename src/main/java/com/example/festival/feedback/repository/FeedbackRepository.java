package com.example.festival.feedback.repository;

import com.example.festival.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // 이 메서드가 있어야 서비스의 getUserFeedbacks가 작동합니다.
    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);
}