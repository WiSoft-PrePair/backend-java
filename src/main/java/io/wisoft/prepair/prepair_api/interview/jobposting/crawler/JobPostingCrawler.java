package io.wisoft.prepair.prepair_api.interview.jobposting.crawler;

import io.wisoft.prepair.prepair_api.interview.jobposting.entity.SourceType;

public interface JobPostingCrawler {

    SourceType supportedSite();

    boolean supports(String url);

    /**
     * URL에서 채용공고 텍스트를 크롤링하여 반환
     */
    String crawl(String url);
}