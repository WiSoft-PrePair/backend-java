package io.wisoft.prepair.prepair_api.interview.jobposting.crawler;

import io.wisoft.prepair.prepair_api.interview.jobposting.entity.SourceType;
import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class JobKoreaCrawler implements JobPostingCrawler {

    private static final Pattern GNO_PATTERN = Pattern.compile("/GI_Read/(\\d+)");
    private static final String DETAIL_IFRAME_URL_TEMPLATE =
            "https://www.jobkorea.co.kr/Recruit/GI_Read_Comt_Ifrm?Gno=%s";

    @Override
    public SourceType supportedSite() {
        return SourceType.JOBKOREA;
    }

    @Override
    public boolean supports(final String url) {
        return url.contains("jobkorea.co.kr");
    }

    @Override
    public String crawl(final String url) {
        final String gno = extractGno(url);
        final String detailUrl = DETAIL_IFRAME_URL_TEMPLATE.formatted(gno);

        try {
            final Document document = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10_000)
                    .get();

            final Element detailContent = document.selectFirst("#detail-content");

            if (detailContent == null) {
                throw new BusinessException(ErrorCode.CRAWLING_FAILED);
            }

            return detailContent.text();
        } catch (final IOException e) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }

    private String extractGno(final String url) {
        final String path = URI.create(url).getPath();
        final Matcher matcher = GNO_PATTERN.matcher(path);

        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return matcher.group(1);
    }
}
