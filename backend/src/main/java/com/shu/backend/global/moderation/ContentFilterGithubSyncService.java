package com.shu.backend.global.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.backend.domain.contentfilter.entity.ContentFilterTerm;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSource;
import com.shu.backend.domain.contentfilter.repository.ContentFilterTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFilterGithubSyncService implements ApplicationRunner {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final ContentFilterProperties properties;
    private final ContentFilterTermRepository termRepository;
    private final TextNormalizationService normalizationService;
    private final ContentFilterDictionaryService dictionaryService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @Scheduled(cron = "${moderation.text.sync-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void scheduledSync() {
        if (!properties.isSyncEnabled()) {
            return;
        }
        syncAllSources();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSyncEnabled()) {
            return;
        }
        try {
            syncAllSources();
        } catch (Exception e) {
            log.warn("Initial content filter sync failed. Existing dictionary cache will be used.", e);
        }
    }

    public void syncAllSources() {
        int sourceCount = 0;
        int termCount = 0;

        for (ContentFilterProperties.Source source : properties.getSources()) {
            if (!isValidSource(source)) {
                continue;
            }

            try {
                int imported = syncSource(source);
                sourceCount++;
                termCount += imported;
            } catch (Exception e) {
                log.warn("Content filter source sync failed. language={}, url={}",
                        source.getLanguage(), source.getUrl(), e);
            }
        }

        dictionaryService.reload();
        log.info("Content filter sync completed. sources={}, terms={}", sourceCount, termCount);
    }

    private boolean isValidSource(ContentFilterProperties.Source source) {
        return source != null
                && source.isEnabled()
                && StringUtils.hasText(source.getLanguage())
                && StringUtils.hasText(source.getUrl());
    }

    private int syncSource(ContentFilterProperties.Source source) throws Exception {
        String body = fetch(source.getUrl());
        String content = extractContent(body);
        Set<String> rawTerms = parseTerms(content);

        Set<String> normalizedTerms = new LinkedHashSet<>();
        for (String term : rawTerms) {
            String normalized = normalizationService.normalize(term);
            if (normalized.length() < 2 || normalized.length() > 255) {
                continue;
            }

            normalizedTerms.add(normalized);
            ContentFilterTerm entity = termRepository
                    .findByLanguageAndNormalizedTerm(source.getLanguage(), normalized)
                    .orElseGet(() -> ContentFilterTerm.builder()
                            .language(source.getLanguage())
                            .term(term)
                            .normalizedTerm(normalized)
                            .severity(source.getSeverity())
                            .category(source.getCategory())
                            .source(ContentFilterSource.GITHUB)
                            .sourceKey(source.getUrl())
                            .enabled(true)
                            .build());

            entity.refresh(
                    term,
                    normalized,
                    source.getSeverity(),
                    source.getCategory(),
                    ContentFilterSource.GITHUB,
                    source.getUrl()
            );
            termRepository.save(entity);
        }

        disableRemovedGithubTerms(source, normalizedTerms);
        return normalizedTerms.size();
    }

    private String fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TeenPle-Content-Filter")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GitHub API returned status " + response.statusCode());
        }
        return response.body();
    }

    private String extractContent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (root.isArray()) {
            return Arrays.stream(objectMapper.treeToValue(root, JsonNode[].class))
                    .map(node -> node.path("url").asText(""))
                    .filter(StringUtils::hasText)
                    .map(url -> {
                        try {
                            return extractContent(fetch(url));
                        } catch (Exception e) {
                            log.warn("Content filter child source sync failed. url={}", url, e);
                            return "";
                        }
                    })
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n"));
        }

        String encoded = root.path("content").asText("");
        if (!StringUtils.hasText(encoded)) {
            throw new IllegalStateException("GitHub content payload has no content field");
        }

        byte[] decoded = Base64.getMimeDecoder().decode(encoded);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private Set<String> parseTerms(String content) {
        return content.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("//"))
                .map(line -> line.replace("\uFEFF", "").trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void disableRemovedGithubTerms(ContentFilterProperties.Source source, Set<String> normalizedTerms) {
        if (normalizedTerms.isEmpty()) {
            return;
        }

        List<ContentFilterTerm> removedTerms = termRepository
                .findBySourceAndSourceKeyAndLanguageAndNormalizedTermNotIn(
                        ContentFilterSource.GITHUB,
                        source.getUrl(),
                        source.getLanguage(),
                        normalizedTerms
                );
        removedTerms.forEach(ContentFilterTerm::disable);
        termRepository.saveAll(removedTerms);
    }
}
