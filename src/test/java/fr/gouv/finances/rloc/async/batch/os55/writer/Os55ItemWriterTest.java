package fr.gouv.finances.rloc.async.batch.os55.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.Os55SurveillanceRepository;
import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55ReponseDTO;
import fr.gouv.finances.rloc.async.service.os55.Os55ResultService;

@ExtendWith(MockitoExtension.class)
class Os55ItemWriterTest {

    @Mock
    private AsyncRequestRepository asyncRequestRepository;
    @Mock
    private Os55SurveillanceRepository surveillanceRepository;
    @Mock
    private Os55ResultService resultService;

    @Test
    void deletesSurveillanceAndSavesOkResult() throws Exception {
        UUID uuid = UUID.randomUUID();
        prepareRequest(uuid);
        when(resultService.existsResult(1L, 1L)).thenReturn(false);
        when(surveillanceRepository.deleteSurveillanceForUpdate("123456", "1001"))
                .thenReturn(Optional.of(new Os55SurveillanceRepository.SuppressionSurveillanceResult(99L, 1)));
        Os55ItemWriter writer = writer(uuid);

        writer.write(new Chunk<>(List.of(validItem())));

        ArgumentCaptor<Os55ReponseDTO> captor = ArgumentCaptor.forClass(Os55ReponseDTO.class);
        verify(resultService).saveResult(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().getCodeRetour()).isEqualTo("OK");
        assertThat(captor.getValue().getIdSurveillance()).isEqualTo(99L);
    }

    @Test
    void skipsAlreadyPersistedResultWithoutDeletingAgain() throws Exception {
        UUID uuid = UUID.randomUUID();
        prepareRequest(uuid);
        when(resultService.existsResult(1L, 1L)).thenReturn(true);

        writer(uuid).write(new Chunk<>(List.of(validItem())));

        verify(surveillanceRepository, never()).deleteSurveillanceForUpdate(any(), any());
        verify(resultService, never()).saveResult(any(), any());
    }

    @Test
    void savesKoResultWithoutDeletingInvalidItem() throws Exception {
        UUID uuid = UUID.randomUUID();
        prepareRequest(uuid);
        when(resultService.existsResult(1L, 1L)).thenReturn(false);
        ErreurDTO erreur = new ErreurDTO();
        erreur.setCodeMotifRejet(5114);
        Os55ProcessedItem rejected = new Os55ProcessedItem(new Os55DemandeDTO(1L, null, "1001"), List.of(erreur));

        writer(uuid).write(new Chunk<>(List.of(rejected)));

        verify(surveillanceRepository, never()).deleteSurveillanceForUpdate(any(), any());
        verify(resultService).saveResult(org.mockito.ArgumentMatchers.eq(1L), any(Os55ReponseDTO.class));
    }

    @Test
    void throwsTechnicalExceptionWhenDeletedRowCountIsInvalid() {
        UUID uuid = UUID.randomUUID();
        prepareRequest(uuid);
        when(resultService.existsResult(1L, 1L)).thenReturn(false);
        when(surveillanceRepository.deleteSurveillanceForUpdate("123456", "1001"))
                .thenReturn(Optional.of(new Os55SurveillanceRepository.SuppressionSurveillanceResult(99L, 2)));

        assertThatThrownBy(() -> writer(uuid).write(new Chunk<>(List.of(validItem()))))
                .isInstanceOf(IllegalStateException.class);
        verify(resultService, never()).saveResult(any(), any());
    }

    private void prepareRequest(UUID uuid) {
        AsyncRequestEntity request = new AsyncRequestEntity();
        request.setId(1L);
        request.setUuid(uuid);
        when(asyncRequestRepository.findByUuid(uuid)).thenReturn(Optional.of(request));
    }

    private Os55ItemWriter writer(UUID uuid) {
        return new Os55ItemWriter(uuid, asyncRequestRepository, surveillanceRepository, resultService,
                new ErreurDTOFactory());
    }

    private Os55ProcessedItem validItem() {
        return new Os55ProcessedItem(new Os55DemandeDTO(1L, "123456", "1001"), List.of());
    }
}
