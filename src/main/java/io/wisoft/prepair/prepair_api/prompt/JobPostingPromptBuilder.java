package io.wisoft.prepair.prepair_api.prompt;

import org.springframework.stereotype.Component;

@Component
public class JobPostingPromptBuilder {

    public String buildStructuringPrompt(final String rawContent) {
        return """
                너는 채용공고 텍스트를 분석하여 구조화된 JSON으로 변환하는 전문가야.
                
                아래 규칙을 따라줘:
                1. 채용공고 본문에서 정보를 추출하여 아래 JSON 형식으로 응답해줘.
                2. 메뉴, 푸터, 광고 등 채용공고와 무관한 내용은 무시해줘.
                3. 정보가 없는 필드는 빈 문자열("")로 남겨줘.
                4. 반드시 아래 JSON 형식만 응답하고, 다른 텍스트는 포함하지 마.
                5. JSON 외의 마크다운(```json 등)도 포함하지 마.
                
                {
                  "company_name": "회사명",
                  "job_title": "채용 포지션명",
                  "responsibilities": "주요 업무 및 담당 역할",
                  "requirements": "자격 요건 (필수 조건)",
                  "preferred_qualifications": "우대 사항",
                  "tech_stack": "기술 스택 (쉼표로 구분)",
                  "experience_level": "경력 수준 (예: 신입, 3년 이상, 시니어)",
                  "employment_type": "고용 형태 (예: 정규직, 계약직, 인턴)",
                  "deadline": "채용 마감일"
                }
                  채용공고 원문:
                      %s
                """.formatted(rawContent);
    }
}
