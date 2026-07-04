package com.shu.backend.global.moderation;

import com.shu.backend.domain.contentfilter.enums.ContentFilterCategory;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSeverity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "moderation.text")
public class ContentFilterProperties {

    private boolean syncEnabled = true;
    private List<Source> sources = new ArrayList<>();

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public static class Source {
        private boolean enabled = true;
        private String language;
        private String url;
        private ContentFilterSeverity severity = ContentFilterSeverity.BLOCK;
        private ContentFilterCategory category = ContentFilterCategory.PROFANITY;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public ContentFilterSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(ContentFilterSeverity severity) {
            this.severity = severity;
        }

        public ContentFilterCategory getCategory() {
            return category;
        }

        public void setCategory(ContentFilterCategory category) {
            this.category = category;
        }
    }
}
