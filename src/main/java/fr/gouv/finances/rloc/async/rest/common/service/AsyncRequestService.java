package fr.gouv.finances.rloc.async.rest.common.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import fr.gouv.finances.rloc.async.config.AsyncRequestProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.exception.BadRequestException;
import fr.gouv.finances.rloc.async.exception.ConflictException;
import fr.gouv.finances.rloc.async.exception.ForbiddenException;
import fr.gouv.finances.rloc.async.exception.NotFoundException;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.rest.common.dto.RequeteDTO;

@Service
public class AsyncRequestService {

    public static final int OS55_CODE = 55;

    private final AsyncRequestRepository repository;
    private final FileStorageService fileStorageService;
    private final RequestStatusService requestStatusService;
    private final AsyncRequestProperties properties;

    public AsyncRequestService(AsyncRequestRepository repository, FileStorageService fileStorageService,
            RequestStatusService requestStatusService, AsyncRequestProperties properties) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
        this.requestStatusService = requestStatusService;
        this.properties = properties;
    }

    @Transactional
    public UUID submit(UUID uuid, String clientId, Long declaredElementCount, InputStream body) {
        validateMetadata(uuid, clientId, declaredElementCount);
        AsyncRequestEntity request = reserve(uuid, clientId, declaredElementCount);
        Path temporaryPath = null;
        try {
            FileStorageService.StoredJsonlFile stored = fileStorageService.writeTemporaryInputFile(uuid, body,
                    properties.maxSizeBytes(), properties.maxElementCount());
            temporaryPath = stored.temporaryPath();
            if (stored.lineCount() != declaredElementCount.longValue()) {
                throw new BadRequestException("Le nombre d'elements annonce ne correspond pas au flux JSONL");
            }
            Path finalPath = fileStorageService.finalizeInputFile(uuid, stored.temporaryPath());
            request.setInputFilePath(finalPath.toString());
            request.setStatus(AsyncRequestStatus.EN_ATTENTE);
            repository.saveAndFlush(request);
            return uuid;
        } catch (RuntimeException exception) {
            if (temporaryPath != null) {
                fileStorageService.deletePathQuietly(temporaryPath);
            }
            request.setStatus(AsyncRequestStatus.ERREUR);
            repository.saveAndFlush(request);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public RequeteDTO getStatus(UUID uuid, String clientId) {
        AsyncRequestEntity request = findOwnedRequest(uuid, clientId);
        return requestStatusService.toPublicDto(request);
    }

    @Transactional(readOnly = true)
    public InputStreamResource openResults(UUID uuid, String clientId) {
        AsyncRequestEntity request = findOwnedRequest(uuid, clientId);
        if (request.getStatus() != AsyncRequestStatus.TERMINE) {
            throw new ConflictException("La demande n'est pas terminee");
        }
        if (!fileStorageService.exists(request.getOutputFilePath())) {
            throw new fr.gouv.finances.rloc.async.exception.StorageException("Fichier de resultats absent");
        }
        return fileStorageService.openResultFile(request.getOutputFilePath());
    }

    @Transactional
    public Map<String, String> acknowledge(UUID uuid, String clientId) {
        AsyncRequestEntity request = findOwnedRequest(uuid, clientId);
        if (request.getStatus() == AsyncRequestStatus.ACQUITTE) {
            return Map.of("uuid", uuid.toString(), "status", AsyncRequestStatus.ACQUITTE.name());
        }
        if (request.getStatus() != AsyncRequestStatus.TERMINE) {
            throw new ConflictException("La demande n'est pas terminee");
        }
        request.setStatus(AsyncRequestStatus.ACQUITTE);
        repository.saveAndFlush(request);
        fileStorageService.deleteRequestFiles(request);
        return Map.of("uuid", uuid.toString(), "status", AsyncRequestStatus.ACQUITTE.name());
    }

    private void validateMetadata(UUID uuid, String clientId, Long declaredElementCount) {
        if (uuid == null) {
            throw new BadRequestException("X-Request-Id est obligatoire");
        }
        if (!StringUtils.hasText(clientId)) {
            throw new BadRequestException("X-Client-Id est obligatoire");
        }
        if (declaredElementCount == null || declaredElementCount <= 0) {
            throw new BadRequestException("X-Element-Count doit etre positif");
        }
        if (declaredElementCount > properties.maxElementCount()) {
            throw new fr.gouv.finances.rloc.async.exception.PayloadTooLargeException(
                    "Nombre maximal d'elements depasse");
        }
    }

    private AsyncRequestEntity reserve(UUID uuid, String clientId, Long declaredElementCount) {
        if (repository.existsByUuid(uuid)) {
            throw new ConflictException("UUID de demande deja existant");
        }
        AsyncRequestEntity request = new AsyncRequestEntity();
        request.setUuid(uuid);
        request.setClientId(clientId);
        request.setCodeOS(OS55_CODE);
        request.setStatus(AsyncRequestStatus.EN_RECEPTION);
        request.setDeclaredElementCount(declaredElementCount);
        request.setProcessedElementCount(0L);
        try {
            return repository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("UUID de demande deja existant");
        }
    }

    private AsyncRequestEntity findOwnedRequest(UUID uuid, String clientId) {
        AsyncRequestEntity request = repository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Demande inconnue"));
        if (!request.getClientId().equals(clientId)) {
            throw new ForbiddenException("Acces interdit a cette demande");
        }
        return request;
    }
}
