package fr.gouv.finances.rloc.async.service.os55;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.finances.rloc.async.config.RlocAsyncProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.entity.AsyncResultatElementEntity;
import fr.gouv.finances.rloc.async.exception.StorageException;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.AsyncResultatElementRepository;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55ReponseDTO;

@Service
public class Os55ResultService {

    private final AsyncResultatElementRepository resultatRepository;
    private final AsyncRequestRepository requestRepository;
    private final RlocAsyncProperties properties;
    private final ObjectMapper objectMapper;

    public Os55ResultService(AsyncResultatElementRepository resultatRepository, AsyncRequestRepository requestRepository,
            RlocAsyncProperties properties, ObjectMapper objectMapper) {
        this.resultatRepository = resultatRepository;
        this.requestRepository = requestRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public boolean existsResult(Long requeteId, Long num) {
        return resultatRepository.existsByRequeteIdAndNum(requeteId, num);
    }

    public void saveResult(Long requeteId, Os55ReponseDTO response) {
        if (existsResult(requeteId, response.getNum())) {
            return;
        }
        AsyncResultatElementEntity entity = new AsyncResultatElementEntity();
        entity.setRequeteId(requeteId);
        entity.setNum(response.getNum());
        entity.setCodeRetour(response.getCodeRetour());
        entity.setResultatJson(toJson(response));
        resultatRepository.save(entity);
    }

    @Transactional
    public Path publishResults(AsyncRequestEntity request) {
        long actualCount = resultatRepository.countByRequeteId(request.getId());
        if (request.getDeclaredElementCount() != null && actualCount != request.getDeclaredElementCount()) {
            throw new IllegalStateException("Nombre de resultats OS55 incoherent pour la demande " + request.getUuid());
        }
        Path tempPath = properties.storage().processingDir().resolve(request.getUuid() + "-result.ndjson.part").normalize();
        Path finalPath = properties.storage().outgoingDir().resolve(request.getUuid() + "-result.ndjson").normalize();
        try {
            Files.createDirectories(tempPath.getParent());
            Files.createDirectories(finalPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8);
                    var results = resultatRepository.streamByRequeteIdOrderByNumAsc(request.getId())) {
                results.forEach(result -> writeLine(writer, result.getResultatJson()));
            }
            Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            request.setOutputFilePath(finalPath.toString());
            request.setStatus(AsyncRequestStatus.TERMINE);
            requestRepository.saveAndFlush(request);
            return finalPath;
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(tempPath);
            throw new StorageException("Impossible de publier le fichier de resultats OS55", exception);
        }
    }

    private String toJson(Os55ReponseDTO response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de serialiser le resultat OS55", exception);
        }
    }

    private void writeLine(BufferedWriter writer, String json) {
        try {
            writer.write(json);
            writer.newLine();
        } catch (IOException exception) {
            throw new StorageException("Impossible d'ecrire le fichier de resultats OS55", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
