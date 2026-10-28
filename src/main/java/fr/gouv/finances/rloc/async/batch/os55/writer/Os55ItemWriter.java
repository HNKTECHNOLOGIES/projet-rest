package fr.gouv.finances.rloc.async.batch.os55.writer;

import java.util.List;
import java.util.UUID;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.Os55SurveillanceRepository;
import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55ReponseDTO;
import fr.gouv.finances.rloc.async.rest.os55.enums.Os55ErreurEnum;
import fr.gouv.finances.rloc.async.service.os55.Os55ResultService;

public class Os55ItemWriter implements ItemWriter<Os55ProcessedItem> {

    private final UUID requestUuid;
    private final AsyncRequestRepository asyncRequestRepository;
    private final Os55SurveillanceRepository surveillanceRepository;
    private final Os55ResultService resultService;
    private final ErreurDTOFactory erreurDTOFactory;

    public Os55ItemWriter(UUID requestUuid, AsyncRequestRepository asyncRequestRepository,
            Os55SurveillanceRepository surveillanceRepository, Os55ResultService resultService,
            ErreurDTOFactory erreurDTOFactory) {
        this.requestUuid = requestUuid;
        this.asyncRequestRepository = asyncRequestRepository;
        this.surveillanceRepository = surveillanceRepository;
        this.resultService = resultService;
        this.erreurDTOFactory = erreurDTOFactory;
    }

    @Override
    public void write(Chunk<? extends Os55ProcessedItem> chunk) {
        AsyncRequestEntity request = asyncRequestRepository.findByUuid(requestUuid).orElseThrow();
        for (Os55ProcessedItem item : chunk.getItems()) {
            Os55DemandeDTO demande = item.demande();
            if (resultService.existsResult(request.getId(), demande.getNum())) {
                continue;
            }
            if (item.isRejected()) {
                resultService.saveResult(request.getId(), buildKoResponse(demande, item.erreurs()));
                continue;
            }
            Os55SurveillanceRepository.SuppressionSurveillanceResult suppression = surveillanceRepository
                    .deleteSurveillanceForUpdate(demande.getIdLocal(), demande.getIdFiche())
                    .orElse(null);
            if (suppression == null) {
                ErreurDTO erreur = erreurDTOFactory.create(Os55ErreurEnum.COUPLE_SURVEILLANCE_INEXISTANT,
                        demande.getIdLocal(), demande.getIdFiche());
                resultService.saveResult(request.getId(), buildKoResponse(demande, List.of(erreur)));
                continue;
            }
            if (suppression.deletedRows() != 1) {
                throw new IllegalStateException("Suppression OS55 incoherente pour num=" + demande.getNum());
            }
            resultService.saveResult(request.getId(), new Os55ReponseDTO(demande.getNum(), "OK",
                    suppression.idSurveillance(), demande.getIdLocal(), demande.getIdFiche(), null));
        }
    }

    private Os55ReponseDTO buildKoResponse(Os55DemandeDTO demande, List<ErreurDTO> erreurs) {
        return new Os55ReponseDTO(demande.getNum(), "KO", null, demande.getIdLocal(), demande.getIdFiche(), erreurs);
    }
}
