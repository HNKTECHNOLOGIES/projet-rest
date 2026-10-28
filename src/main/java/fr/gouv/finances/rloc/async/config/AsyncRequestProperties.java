package fr.gouv.finances.rloc.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rloc.async-request")
public record AsyncRequestProperties(long maxSizeBytes, long maxElementCount) {
}
