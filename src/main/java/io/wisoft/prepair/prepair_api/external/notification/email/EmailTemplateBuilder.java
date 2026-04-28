package io.wisoft.prepair.prepair_api.external.notification.email;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildInterviewQuestionHtml(String recipientName, String questionTag, String question) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f6f8fa;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="font-family:'Apple SD Gothic Neo','Noto Sans KR',sans-serif; background-color:#f6f8fa; padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff; border-radius:12px; padding:40px; box-shadow:0 4px 12px rgba(0,0,0,0.05);">

                        <tr>
                          <td align="center" style="padding-bottom:24px;">
                            <img src="https://prepair.wisoft.dev/assets/logo-EOf_Dr2C.png" alt="PrePair" width="120" />
                          </td>
                        </tr>

                        <tr>
                          <td align="center" style="font-size:24px; font-weight:700; color:#1a1a1a; padding-bottom:8px;">
                            %s님, 오늘의 면접 질문이에요!
                          </td>
                        </tr>

                        <tr>
                          <td align="center" style="font-size:14px; color:#666666; padding-bottom:32px;">
                            매일 꾸준히 면접 준비하는 당신을 응원합니다 :)
                          </td>
                        </tr>

                        <tr>
                          <td style="padding:24px; background:linear-gradient(135deg,#f0f4ff 0%%,#e8f0fe 100%%); border-radius:12px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td align="center" style="padding-bottom:16px;">
                                  <span style="display:inline-block; background:#405173; color:white;
                                               padding:6px 16px; border-radius:20px; font-size:13px; font-weight:600;">
                                    #%s
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td align="center" style="font-size:18px; color:#1a1a1a; line-height:1.7; padding:0 16px;">
                                  &ldquo;%s&rdquo;
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <tr>
                          <td align="center" style="padding-top:32px;">
                            <a href="https://prepair.wisoft.dev"
                               style="display:inline-block; background:#405173; color:white;
                                      padding:14px 32px; border-radius:8px; text-decoration:none;
                                      font-weight:600; font-size:16px;">
                              답변하러 가기
                            </a>
                          </td>
                        </tr>

                        <tr>
                          <td align="center" style="padding-top:40px; font-size:12px; color:#999999;">
                            이 메일은 PrePair 알림 설정에 따라 발송되었습니다.<br/>
                            알림 설정은 마이페이지에서 변경할 수 있습니다.
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(recipientName, questionTag, question);
    }
}
