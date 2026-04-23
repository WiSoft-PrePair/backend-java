package io.wisoft.prepair.prepair_api.repository;

import io.wisoft.prepair.prepair_api.entity.InterviewSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<InterviewSession, UUID> {
    boolean existsByIdAndMemberId(UUID id, UUID memberId);
}