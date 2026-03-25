package io.wisoft.prepair.prepair_api.entity;

import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "interview_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private InterviewAnswer interviewAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    @Column
    private Integer score;

    public InterviewFeedback(
            final InterviewAnswer interviewAnswer,
            final String feedback,
            final FeedbackType feedbackType,
            final Integer score
    ) {
        this.interviewAnswer = interviewAnswer;
        this.feedback = feedback;
        this.feedbackType = feedbackType;
        this.score = score;

    }
}
