package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<InterviewAnswer, UUID> {
}
