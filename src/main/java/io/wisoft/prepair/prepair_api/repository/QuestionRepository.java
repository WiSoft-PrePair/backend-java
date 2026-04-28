package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findByMemberId(UUID memberId);

    List<InterviewQuestion> findByMemberIdAndQuestionTypeOrderByCreatedAtDesc(UUID memberId, QuestionType questionType);

    Optional<InterviewQuestion> findByIdAndMemberId(UUID id, UUID memberId);

    List<InterviewQuestion> findByInterviewSessionId(UUID sessionId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE InterviewQuestion q SET q.latestScore = :score " +
           "WHERE q.id = :id " +
           "AND q.latestScore IS NULL " +
           "AND q.questionType = io.wisoft.prepair.prepair_api.entity.enums.QuestionType.TEXT " +
           "AND q.createdAt >= :startOfDay AND q.createdAt < :endOfDay")
    int updateLatestScoreIfFirstTime(
            @Param("id") UUID id,
            @Param("score") Integer score,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
