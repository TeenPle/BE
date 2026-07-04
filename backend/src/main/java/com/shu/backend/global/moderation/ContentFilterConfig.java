package com.shu.backend.global.moderation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContentFilterProperties.class)
public class ContentFilterConfig {
}
