package com.example.festival.feedback.repository;

import com.example.festival.feedback.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCoverLetterIdOrderByQuestionNumber(Long coverLetterId);
}