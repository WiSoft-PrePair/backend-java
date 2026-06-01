package io.wisoft.prepair.prepair_api.interview.session.repository;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<InterviewSession, UUID> {
    List<InterviewSession> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
}