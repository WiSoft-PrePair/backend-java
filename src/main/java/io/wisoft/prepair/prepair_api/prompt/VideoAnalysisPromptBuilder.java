package io.wisoft.prepair.prepair_api.prompt;

import org.springframework.stereotype.Component;

@Component
public class VideoAnalysisPromptBuilder {

    public String buildVisionPrompt() {
        return """
                너는 면접 비언어적 커뮤니케이션 분석 전문가야.
                아래 면접 영상에서 추출된 프레임들을 분석하여 비언어적 요소를 평가해줘.

                분석 항목:
                1. eye_contact: 시선 처리 (카메라/면접관을 향한 시선 유지 정도)
                2. facial_expression: 표정 (자연스러움, 자신감, 긴장도)
                3. posture: 자세 (바른 자세, 안정감)
                4. gesture: 제스처 (적절한 손동작, 과도한 움직임 여부)

                규칙:
                1. 각 항목에 대해 0~100 사이 점수를 매겨줘.
                2. 각 항목에 대한 구체적 피드백을 작성해줘.
                3. 전체 종합 점수(overall_score)를 매겨줘.
                4. 반드시 아래 JSON 형식으로만 응답하고, 마크다운은 포함하지 마.

                {
                  "eye_contact": { "score": 80, "feedback": "피드백 내용" },
                  "facial_expression": { "score": 75, "feedback": "피드백 내용" },
                  "posture": { "score": 85, "feedback": "피드백 내용" },
                  "gesture": { "score": 70, "feedback": "피드백 내용" },
                  "overall_score": 78,
                  "summary": "종합 피드백"
                }
                """;
    }
}