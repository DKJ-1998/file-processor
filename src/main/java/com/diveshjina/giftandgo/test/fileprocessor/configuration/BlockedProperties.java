package com.diveshjina.giftandgo.test.fileprocessor.configuration;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blocked")
public record BlockedProperties(Set<String> countries, Set<String> isps) {
}
