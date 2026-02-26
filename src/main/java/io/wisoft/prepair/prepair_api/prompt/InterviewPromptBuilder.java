package io.wisoft.prepair.prepair_api.prompt;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewPromptBuilder {

    public String buildDailyQuestionPrompt(String job, List<InterviewQuestion> previousQuestions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 ").append(job).append(" 면접관입니다.\n");

        if (!previousQuestions.isEmpty()) {
            prompt.append("\n이전에 받은 질문들:\n");
            previousQuestions.forEach(q ->
                    prompt.append("- ").append(q.getQuestion()).append("\n")
            );
            prompt.append("\n위 질문들을 제외하고 ");
        }

        prompt.append("오늘의 기술 면접 질문을 만들고, ");
        prompt.append("관련 키워드 태그 2-3개를 추출해주세요.\n\n");

        prompt.append("응답은 다음 JSON 형식으로만 작성해주세요:\n");
        prompt.append("{\n");
        prompt.append("  \"question\": \"질문 내용\",\n");
        prompt.append("  \"tags\": [\"태그1\", \"태그2\", \"태그3\"]\n");
        prompt.append("}");

        return prompt.toString();
    }

    public String buildCompanyQuestionPrompt(String jobPostingContent) {
        return """  
                너는 채용공고 내용을 분석하여 실제 그 기업의 면접을 진행하는 면접관이야.
                
                아래 채용공고 정보를 바탕으로 지원자에게 물어볼 면접 질문 5개를 만들고, 관련 키워드 태그 2-3개를 추출해줘.
                
                규칙:
                1. 채용공고의 기술 스택, 자격 요건, 담당 업무를 반드시 반영해줘.
                2. 반드시 아래 JSON 형식으로만 응답하고, 다른 텍스트는 포함하지 마.
                3. JSON 외의 마크다운(```json 등)도 포함하지 마.
                [
                  {
                    "question": "질문 내용",
                    "tags": ["태그1", "태그2", "태그3"]
                  },
                  {
                    "question": "질문 내용",
                    "tags": ["태그1", "태그2", "태그3"]
                  },
                  {
                    "question": "질문 내용",
                    "tags": ["태그1", "태그2", "태그3"]
                  },
                  {
                    "question": "질문 내용",
                    "tags": ["태그1", "태그2", "태그3"]
                  },
                  {
                    "question": "질문 내용",
                    "tags": ["태그1", "태그2", "태그3"]
                  }
                ]
                
                채용공고 정보:
                %s
                """.formatted(jobPostingContent);
    }


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
                    {
                      "good": "잘한 점",
                      "improvement": "부족한 점",
                      "recommendation": "개선 방향",
                      "score": 85
                    }

                질문: %s
                답변: %s
                """.formatted(questionTag, question, answer);
    }
}
