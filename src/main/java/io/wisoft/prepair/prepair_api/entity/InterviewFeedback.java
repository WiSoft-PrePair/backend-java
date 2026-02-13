package io.wisoft.prepair.prepair_api.entity;

import io.wisoft.prepair.prepair_api.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column
    private Integer score;

    public InterviewFeedback(
            final InterviewAnswer interviewAnswer,
            final String feedback,
            final Integer score
    ) {
        this.interviewAnswer = interviewAnswer;
        this.feedback = feedback;
        this.score = score;
    }
}
