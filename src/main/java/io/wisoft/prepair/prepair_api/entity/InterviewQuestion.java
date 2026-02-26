package io.wisoft.prepair.prepair_api.entity;

import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "interview_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private String questionTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status = QuestionStatus.UNANSWERED;

    @Column
    private Integer latestScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;

    public InterviewQuestion(
            final UUID memberId,
            final String question,
            final QuestionType questionType,
            final String questionTag,
            final JobPosting jobPosting
    ) {
        this.memberId = memberId;
        this.question = question;
        this.questionType = questionType;
        this.questionTag = questionTag;
        this.jobPosting = jobPosting;
    }

    public void updateStatus(final QuestionStatus status) {
        this.status = status;
    }

    public void updateLatestScore(final Integer score) {
        this.latestScore = score;
    }
}
