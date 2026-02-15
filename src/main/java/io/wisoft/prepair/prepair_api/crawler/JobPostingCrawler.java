package io.wisoft.prepair.prepair_api.crawler;

import io.wisoft.prepair.prepair_api.entity.enums.SourceType;

public interface JobPostingCrawler {

    SourceType supportedSite();

    boolean supports(String url);

    /**
     * URL에서 채용공고 텍스트를 크롤링하여 반환
     */
    String crawl(String url);
}