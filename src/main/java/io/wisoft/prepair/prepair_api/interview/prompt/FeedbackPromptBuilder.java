package io.wisoft.prepair.prepair_api.interview.prompt;

import io.wisoft.prepair.prepair_api.interview.answer.dto.internal.FinalFeedbackInput;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedbackPromptBuilder {

    public String buildFeedbackPrompt(String question, String questionTag, String answer) {
        return """
                너는 %s 관련 직무 분야의 면접 전문가야. 질문의 맥락을 먼저 파악한 후, 해당 분야의 기준에 맞춰 답변을 평가해줘.

                규칙:
                1. good: 답변에서 잘한 점을 작성해줘.
                2. improvement: 부족한 부분을 구체적으로 작성해줘.
                3. recommendation: 개선 방향을 제안해줘.
                4. score: 0~100 사이 정수로 점수를 매겨줘.
                5. 답변이 질문과 무관하거나 내용이 없는 경우, score는 0~10점으로 매기고 good은 "없음"으로 작성해줘.
                6. 반드시 아래 JSON 형식으로만 응답하고, 마크다운(```json 등)은 포함하지 마.
                    {\
                      "good": "잘한 점",
                      "improvement": "부족한 점",
                      "recommendation": "개선 방향",
                      "score": 85
                    }

                질문: %s
                답변: %s
                """.formatted(questionTag, question, answer);
    }

    public String buildCombinedFeedbackPrompt(String question, String sttFeedback, String videoFeedback) {
        return """
                너는 면접 평가 전문가야. 아래 면접 질문에 대한 음성 분석(STT) 피드백과 영상 분석(Video) 피드백을 종합하여 한 줄 종합 평가를 작성해줘.

                규칙:
                1. 음성 분석과 영상 분석의 내용을 모두 고려하여 종합적으로 평가해줘.
                2. combineFeedback: 한 줄로 종합 평가를 작성해줘.
                3. score: 0~100 사이 정수로 종합 점수를 매겨줘.
                4. 반드시 아래 JSON 형식으로만 응답하고, 마크다운(```json 등)은 포함하지 마.
                    {
                      "combineFeedback": "한 줄 종합 평가",
                      "score": 85
                    }

                면접 질문: %s

                음성 분석 피드백:
                %s

                영상 분석 피드백:
                %s
                """.formatted(question, sttFeedback, videoFeedback);
    }

    public String buildFinalFeedbackPrompt(List<FinalFeedbackInput> inputs) {
        String questionsAndFeedbacks = inputs.stream()
                .map(input -> """
                        질문: %s
                        종합 평가: %s
                        점수: %d

                        """.formatted(
                        input.question(),
                        input.combinedFeedback(),
                        input.score()
                ))
                .collect(Collectors.joining());

        return """
                너는 면접 평가 전문가야. 아래는 한 면접 세션에서 진행된 모든 질문과 각 질문에 대한 종합 평가 결과야.
                이를 바탕으로 면접자의 전체적인 면접 수행에 대한 최종 평가 요약을 작성해줘.

                규칙:
                1. 전체 질문에 대한 답변 경향, 강점, 약점을 종합적으로 분석해줘.
                2. finalFeedback: 2-3문장으로 최종 평가 요약을 작성해줘.
                3. 반드시 아래 JSON 형식으로만 응답하고, 마크다운(```json 등)은 포함하지 마.
                    {
                      "finalFeedback": "최종 평가 요약"
                    }

                면접 질문 및 종합 평가 결과:
                %s
                """.formatted(questionsAndFeedbacks);
    }
}
