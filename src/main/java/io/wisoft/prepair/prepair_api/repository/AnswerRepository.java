package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnswerRepository extends JpaRepository<InterviewAnswer, UUID> {

    @Query("SELECT a FROM InterviewAnswer a " +
            "JOIN FETCH a.interviewQuestion q " +
            "JOIN FETCH q.interviewSession " +
            "WHERE a.id = :answerId")
    Optional<InterviewAnswer> findByIdWithQuestionAndSession(UUID answerId);

    Optional<InterviewAnswer> findByInterviewQuestionId(UUID questionId);
}
