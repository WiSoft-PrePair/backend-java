package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.crawler.CrawlerFactory;
import io.wisoft.prepair.prepair_api.crawler.JobPostingCrawler;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.SourceType;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.prompt.JobPostingPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final CrawlerFactory crawlerFactory;
    private final OpenAiClient openAiClient;
    private final JobPostingPromptBuilder promptBuilder;

    @Transactional
    public JobPosting crawlAndSave(final String url) {
        return jobPostingRepository.findBySourceUrl(url)
                .orElseGet(() -> {
                    final JobPostingCrawler crawler = crawlerFactory.getCrawler(url);
                    final String rawContent = crawler.crawl(url);
                    final String content = openAiClient.generateText(promptBuilder.buildStructuringPrompt(rawContent));
                    final SourceType sourceType = crawler.supportedSite();
                    return jobPostingRepository.save(new JobPosting(url, sourceType, content, rawContent));
                });
    }
}