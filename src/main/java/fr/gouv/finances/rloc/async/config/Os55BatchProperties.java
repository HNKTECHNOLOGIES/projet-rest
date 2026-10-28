package fr.gouv.finances.rloc.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rloc.os55.batch")
public record Os55BatchProperties(int chunkSize) {
}
