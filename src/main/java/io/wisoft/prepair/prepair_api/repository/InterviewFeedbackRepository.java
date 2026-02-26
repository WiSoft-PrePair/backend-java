package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {
}
