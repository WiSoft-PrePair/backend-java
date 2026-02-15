package io.wisoft.prepair.prepair_api.client.user;

import io.wisoft.prepair.prepair_api.client.user.dto.UserInfo;
import io.wisoft.prepair.prepair_api.client.user.dto.UserServiceResponse;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${external.user-service.url}")
    private String userServiceUrl;

    public String getJob(UUID userUuid) {
        try {
            String url = userServiceUrl + "/api/members/" + userUuid;
            log.info("User 서비스 호출 - URL: {}", url);

            UserServiceResponse response = restTemplate
                    .getForObject(url, UserServiceResponse.class);

            if (response == null || response.data() == null) {
                log.error("User 서비스 응답이 null입니다. - userId: {}", userUuid);
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            UserInfo userInfo = response.data();
            String job = userInfo.job();

            if (job == null || job.isBlank()) {
                log.error("유저의 직무 정보가 없습니다. - userId: {}", userUuid);
                throw new BusinessException(ErrorCode.USER_JOB_NOT_FOUND);
            }

            log.info("직무 조회 성공 : userId: {}, job: {}", userUuid, job);
            return job;

        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            log.error("유저 없음 - userId: {}", userUuid);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        } catch (HttpServerErrorException e) {
            log.error("User 서비스 에러 - userId: {}", userUuid, e);
            throw new BusinessException(ErrorCode.USER_SERVICE_ERROR);
        } catch (ResourceAccessException e) {
            log.error("User 서비스 연결 실패 - userId: {}", userUuid, e);
            throw new BusinessException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("예상치 못한 에러 - userId: {}", userUuid, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
