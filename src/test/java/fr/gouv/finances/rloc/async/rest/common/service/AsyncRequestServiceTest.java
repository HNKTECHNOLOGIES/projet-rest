package fr.gouv.finances.rloc.async.rest.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.gouv.finances.rloc.async.config.AsyncRequestProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.exception.StorageException;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;

@ExtendWith(MockitoExtension.class)
class AsyncRequestServiceTest {

    @Mock
    private AsyncRequestRepository repository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RequestStatusService requestStatusService;

    @Captor
    private ArgumentCaptor<AsyncRequestEntity> requestCaptor;

    @Test
    void marksRequestAsErrorWhenFileWritingFails() {
        AsyncRequestProperties properties = new AsyncRequestProperties(1024, 10);
        AsyncRequestService service = new AsyncRequestService(repository, fileStorageService, requestStatusService,
                properties);
        UUID uuid = UUID.randomUUID();
        when(repository.existsByUuid(uuid)).thenReturn(false);
        when(repository.saveAndFlush(any(AsyncRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorageService.writeTemporaryInputFile(eq(uuid), any(ByteArrayInputStream.class), eq(1024L), eq(10L)))
                .thenThrow(new StorageException("boom"));

        assertThatThrownBy(() -> service.submit(uuid, "client-a", 1L,
                new ByteArrayInputStream("{}\n".getBytes())))
                .isInstanceOf(StorageException.class);

        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(1).getStatus()).isEqualTo(AsyncRequestStatus.ERREUR);
    }
}
