package io.wisoft.prepair.prepair_api.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.wisoft.prepair.prepair_api.entity.enums.SourceType;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScreenshotCrawler implements JobPostingCrawler {

    @Override
    public SourceType supportedSite() {
        return SourceType.OTHER;
    }

    @Override
    public boolean supports(final String url) {
        return true;
    }

    @Override
    public String crawl(final String url) {
        try (final Playwright playwright = Playwright.create();
             final Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setArgs(List.of("--disable-blink-features=AutomationControlled")))) {

            final var context = browser.newContext(new NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"));

            final Page page = context.newPage();
            page.navigate(url);
            page.waitForLoadState();

            return page.innerText("body");
        } catch (final Exception e) {
            log.error("페이지 텍스트 추출 실패: {} - " + e.getClass().getSimpleName() + ": " + e.getMessage(), url);
            throw new BusinessException(ErrorCode.CRAWLING_FAILED);
        }
    }
}
