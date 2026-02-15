package io.wisoft.prepair.prepair_api.crawler;

import io.wisoft.prepair.prepair_api.entity.enums.SourceType;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class SaraminCrawler implements JobPostingCrawler {

    private static final String DETAIL_URL_TEMPLATE =
            "https://www.saramin.co.kr/zf_user/jobs/relay/view-detail?rec_idx=%s&rec_seq=0";

    @Override
    public SourceType supportedSite() {
        return SourceType.SARAMIN;
    }

    @Override
    public boolean supports(final String url) {
        return url.contains("saramin.co.kr");
    }

    @Override
    public String crawl(final String url) {
        final String recIdx = extractRecIdx(url);
        final String detailUrl = DETAIL_URL_TEMPLATE.formatted(recIdx);

        try {
            final Document document = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10_000)
                    .get();

            final Element userContent = document.selectFirst(".user_content");
            if (userContent == null) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }

            return userContent.text();
        } catch (final IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractRecIdx(final String url) {
        final String query = URI.create(url).getQuery();
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return Arrays.stream(query.split("&"))
                .filter(param -> param.startsWith("rec_idx="))
                .map(param -> param.substring("rec_idx=".length()))
                .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }
}