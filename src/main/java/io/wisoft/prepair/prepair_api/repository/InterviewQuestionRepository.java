package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findByMemberId(UUID memberId);

    List<InterviewQuestion> findByMemberIdAndQuestionTypeOrderByCreatedAtDesc(UUID memberId, QuestionType questionType);

    Optional<InterviewQuestion> findByIdAndMemberId(UUID id, UUID memberId);
}
