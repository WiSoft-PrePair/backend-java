package io.wisoft.prepair.prepair_api.interview.session.repository;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<InterviewSession, UUID> {
}