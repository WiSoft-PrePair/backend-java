package io.wisoft.prepair.prepair_api.global.client.member;

import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MembersData;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberServiceResponse;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberServiceClient {

    private final RestClient restClient;

    @Value("${external.member-service.url}")
    private String memberServiceUrl;

    @Value("${external.member-service.api-key}")
    private String apiKey;

    public List<MemberSchedulerInfo> getMembers() {
        try {
            String url = memberServiceUrl + "/api/members/all";

            MemberServiceResponse<MembersData> response = restClient.get()
                    .uri(url)
                    .header("x-api-prepair", apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.data() == null || response.data().members() == null) {
                log.error("Member 서비스 응답 없음");
                throw new BusinessException(ErrorCode.MEMBER_SERVICE_ERROR);
            }

            return response.data().members();

        } catch (HttpServerErrorException e) {
            log.error("Member 서비스 오류", e);
            throw new BusinessException(ErrorCode.MEMBER_SERVICE_ERROR);
        } catch (ResourceAccessException e) {
            log.error("Member 서비스 연결 실패", e);
            throw new BusinessException(ErrorCode.MEMBER_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Member 서비스 호출 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void sendScore(final UUID memberId, final Integer score) {
        try {
            String url = memberServiceUrl + "/api/members/reward";

            restClient.patch()
                    .uri(url)
                    .header("x-api-prepair", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("memberId", memberId, "score", score))
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpServerErrorException e) {
            log.error("Member 서비스 오류", e);
            throw new BusinessException(ErrorCode.MEMBER_SERVICE_ERROR);
        } catch (ResourceAccessException e) {
            log.error("Member 서비스 연결 실패", e);
            throw new BusinessException(ErrorCode.MEMBER_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Member 서비스 호출 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
