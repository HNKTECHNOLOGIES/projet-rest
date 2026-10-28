package fr.gouv.finances.rloc.async.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rloc")
public record RlocAsyncProperties(Storage storage, Batch batch, Api api) {

    public record Storage(Path incomingDir, Path processingDir, Path outgoingDir, Path archiveDir) {
    }

    public record Batch(int maxConcurrentJobs) {
    }

    public record Api(long maxRequestSizeBytes) {
    }
}
