package io.wisoft.prepair.prepair_api.interview.answer.entity;

import io.wisoft.prepair.prepair_api.interview.answer.entity.AnswerType;
import io.wisoft.prepair.prepair_api.common.support.BaseTimeEntity;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
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
@Table(name = "interview_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerType answerType;

    @Column(columnDefinition = "TEXT")
    private String mediaUrl;

    public InterviewAnswer(
            final InterviewQuestion interviewQuestion,
            final String answer,
            final AnswerType answerType,
            final String mediaUrl
    ) {
        this.interviewQuestion = interviewQuestion;
        this.answer = answer;
        this.answerType = answerType;
        this.mediaUrl = mediaUrl;
    }

    public void updateAnswer(final String answer) {
        this.answer = answer;
    }

    public void updateMediaUrl(final String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
