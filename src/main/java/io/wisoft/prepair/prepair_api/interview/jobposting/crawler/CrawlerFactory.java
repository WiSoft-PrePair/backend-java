package io.wisoft.prepair.prepair_api.interview.jobposting.crawler;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CrawlerFactory {

    private final List<JobPostingCrawler> crawlers;
    private final ScreenshotCrawler screenshotCrawler;

    public CrawlerFactory(final List<JobPostingCrawler> crawlers, final ScreenshotCrawler screenshotCrawler) {
        this.crawlers = crawlers.stream()
                .filter(c -> !(c instanceof ScreenshotCrawler))
                .toList();
        this.screenshotCrawler = screenshotCrawler;
    }

    public JobPostingCrawler getCrawler(final String url) {
        return crawlers.stream()
                .filter(crawler -> crawler.supports(url))
                .findFirst()
                .orElse(screenshotCrawler);
    }
}
