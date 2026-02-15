package io.wisoft.prepair.prepair_api.prompt;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewPromptBuilder {


    public String buildQuestionPrompt(String job, List<InterviewQuestion> previousQuestions) {
        StringBuilder prompt = new StringBuilder();

        // 1. 역할 정의
        prompt.append("당신은 ").append(job).append(" 면접관입니다.\n");

        // 2. 이전 질문 제외 (중복 방지)
        if (!previousQuestions.isEmpty()) {
            prompt.append("\n이전에 받은 질문들:\n");
            previousQuestions.forEach(q ->
                    prompt.append("- ").append(q.getQuestion()).append("\n")
            );
            prompt.append("\n위 질문들을 제외하고 ");
        }

        // 3. 요청 내용
        prompt.append("오늘의 기술 면접 질문을 만들고, ");
        prompt.append("관련 키워드 태그 2-3개를 추출해주세요.\n\n");

        // 4. JSON 형식 지정 (OpenAI가 구조화된 응답을 반환하도록)
        prompt.append("응답은 다음 JSON 형식으로만 작성해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"question\": \"질문 내용\",\n");
        prompt.append("  \"tags\": [\"태그1\", \"태그2\", \"태그3\"]\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 답변 피드백 생성 프롬프트 (나중에 구현)
     */
    public String buildFeedbackPrompt(String question, String answer) {
        // TODO: 피드백 기능 구현 시 작성
        return "";
    }
}
