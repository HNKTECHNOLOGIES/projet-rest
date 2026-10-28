package fr.gouv.finances.rloc.async.rest.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import fr.gouv.finances.rloc.async.config.RlocAsyncProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.exception.PayloadTooLargeException;
import fr.gouv.finances.rloc.async.exception.StorageException;
import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

    private static final int BUFFER_SIZE = 8192;

    private final RlocAsyncProperties properties;

    public FileStorageService(RlocAsyncProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void createStorageDirectories() {
        try {
            Files.createDirectories(properties.storage().incomingDir());
            Files.createDirectories(properties.storage().processingDir());
            Files.createDirectories(properties.storage().outgoingDir());
            Files.createDirectories(properties.storage().archiveDir());
        } catch (IOException exception) {
            throw new StorageException("Impossible de creer les repertoires de stockage", exception);
        }
    }

    public StoredJsonlFile writeTemporaryInputFile(UUID uuid, InputStream inputStream, long maxSizeBytes,
            long maxElementCount) {
        Path temporaryPath = properties.storage().processingDir().resolve(uuid + ".ndjson.part").normalize();
        assertInternalPath(temporaryPath, properties.storage().processingDir());
        long bytes = 0;
        long lines = 0;
        int lastByte = -1;

        try (InputStream in = inputStream; var out = Files.newOutputStream(temporaryPath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                bytes += read;
                if (bytes > maxSizeBytes) {
                    throw new PayloadTooLargeException("Flux JSONL trop volumineux");
                }
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == '\n') {
                        lines++;
                        if (lines > maxElementCount) {
                            throw new PayloadTooLargeException("Nombre maximal d'elements depasse");
                        }
                    }
                    lastByte = buffer[i];
                }
                out.write(buffer, 0, read);
            }
            if (bytes > 0 && lastByte != '\n') {
                lines++;
            }
            if (lines > maxElementCount) {
                throw new PayloadTooLargeException("Nombre maximal d'elements depasse");
            }
            return new StoredJsonlFile(temporaryPath, bytes, lines);
        } catch (PayloadTooLargeException exception) {
            deletePathQuietly(temporaryPath);
            throw exception;
        } catch (IOException exception) {
            deletePathQuietly(temporaryPath);
            throw new StorageException("Impossible d'ecrire le fichier temporaire", exception);
        }
    }

    public Path finalizeInputFile(UUID uuid, Path temporaryPath) {
        assertInternalPath(temporaryPath.normalize(), properties.storage().processingDir());
        Path finalPath = properties.storage().incomingDir().resolve(uuid + ".ndjson").normalize();
        assertInternalPath(finalPath, properties.storage().incomingDir());
        try {
            return Files.move(temporaryPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new StorageException("Impossible de publier le fichier d'entree", exception);
        }
    }

    public boolean exists(String path) {
        return path != null && Files.exists(Path.of(path));
    }

    public InputStreamResource openResultFile(String outputFilePath) {
        if (outputFilePath == null) {
            throw new StorageException("Chemin de sortie absent");
        }
        Path path = Path.of(outputFilePath).normalize();
        assertInternalPath(path, properties.storage().outgoingDir());
        try {
            return new InputStreamResource(Files.newInputStream(path));
        } catch (IOException exception) {
            throw new StorageException("Impossible d'ouvrir le fichier de resultats", exception);
        }
    }

    public void deleteRequestFiles(AsyncRequestEntity request) {
        deletePathQuietly(request.getInputFilePath());
        deletePathQuietly(request.getOutputFilePath());
    }

    public void deletePathQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Nettoyage best effort ; une purge differee reprendra les fichiers restants.
            }
        }
    }

    private void deletePathQuietly(String path) {
        if (path != null) {
            deletePathQuietly(Path.of(path));
        }
    }

    private void assertInternalPath(Path path, Path expectedParent) {
        Path absoluteParent = expectedParent.toAbsolutePath().normalize();
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!absolutePath.startsWith(absoluteParent)) {
            throw new StorageException("Chemin de stockage invalide");
        }
    }

    public void deleteStorageDirectoryForTests() throws IOException {
        Path parent = properties.storage().incomingDir().getParent();
        if (parent != null && Files.exists(parent)) {
            try (var paths = Files.walk(parent)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    public record StoredJsonlFile(Path temporaryPath, long bytes, long lineCount) {
    }
}
