package io.wisoft.prepair.prepair_api.entity;

import io.wisoft.prepair.prepair_api.entity.enums.SessionStatus;
import io.wisoft.prepair.prepair_api.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "interview_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private int totalQuestionCount;

    @Column
    private Integer finalScore;

    @Column(columnDefinition = "TEXT")
    private String finalFeedback;

    public InterviewSession(UUID memberId, int totalQuestionCount) {
        this.memberId = memberId;
        this.totalQuestionCount = totalQuestionCount;
        this.status = SessionStatus.IN_PROGRESS;
    }

    public void complete(int finalScore, String finalFeedback) {
        this.finalScore = finalScore;
        this.finalFeedback = finalFeedback;
        this.status = SessionStatus.COMPLETED;
    }

    public void fail() {
        this.status = SessionStatus.FAILED;
    }
}