package fr.gouv.finances.rloc.async.service.os55;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.finances.rloc.async.config.RlocAsyncProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.entity.AsyncResultatElementEntity;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.AsyncResultatElementRepository;

@ExtendWith(MockitoExtension.class)
class Os55ResultServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private AsyncResultatElementRepository resultatRepository;
    @Mock
    private AsyncRequestRepository requestRepository;

    @Test
    void publishesFinalJsonlFromPersistedResults() throws Exception {
        AsyncRequestEntity request = new AsyncRequestEntity();
        request.setId(1L);
        request.setUuid(UUID.randomUUID());
        request.setDeclaredElementCount(2L);
        AsyncResultatElementEntity first = result(1L, "{\"num\":1,\"codeRetour\":\"OK\"}");
        AsyncResultatElementEntity second = result(2L, "{\"num\":2,\"codeRetour\":\"KO\"}");
        when(resultatRepository.countByRequeteId(1L)).thenReturn(2L);
        when(resultatRepository.streamByRequeteIdOrderByNumAsc(1L)).thenReturn(Stream.of(first, second));
        Os55ResultService service = new Os55ResultService(resultatRepository, requestRepository, properties(),
                new ObjectMapper());

        Path finalPath = service.publishResults(request);

        assertThat(Files.readString(finalPath)).isEqualTo("""
                {"num":1,"codeRetour":"OK"}
                {"num":2,"codeRetour":"KO"}
                """);
        assertThat(request.getStatus()).isEqualTo(AsyncRequestStatus.TERMINE);
        assertThat(request.getOutputFilePath()).isEqualTo(finalPath.toString());
        verify(requestRepository).saveAndFlush(request);
    }

    private AsyncResultatElementEntity result(Long num, String json) {
        AsyncResultatElementEntity entity = new AsyncResultatElementEntity();
        entity.setRequeteId(1L);
        entity.setNum(num);
        entity.setCodeRetour(json.contains("OK") ? "OK" : "KO");
        entity.setResultatJson(json);
        return entity;
    }

    private RlocAsyncProperties properties() {
        return new RlocAsyncProperties(
                new RlocAsyncProperties.Storage(tempDir.resolve("incoming"), tempDir.resolve("processing"),
                        tempDir.resolve("outgoing"), tempDir.resolve("archive")),
                new RlocAsyncProperties.Batch(2), new RlocAsyncProperties.Api(1024));
    }
}
