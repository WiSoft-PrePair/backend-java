package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {
    List<InterviewFeedback> findByInterviewAnswerId(UUID answerId);

    @Query("SELECT COUNT(f) FROM InterviewFeedback f " +
            "WHERE f.interviewAnswer.interviewQuestion.interviewSession.id = :sessionId " +
            "AND f.feedbackType = :feedbackType")
    long countBySessionIdAndFeedbackType(UUID sessionId, FeedbackType feedbackType);

    Optional<InterviewFeedback> findByInterviewAnswerIdAndFeedbackType(UUID answerId, FeedbackType feedbackType);
}
