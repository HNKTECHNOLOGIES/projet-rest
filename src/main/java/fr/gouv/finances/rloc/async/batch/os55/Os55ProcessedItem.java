package fr.gouv.finances.rloc.async.batch.os55;

import java.util.List;

import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;

public record Os55ProcessedItem(Os55DemandeDTO demande, List<ErreurDTO> erreurs) {

    public boolean isRejected() {
        return erreurs != null && !erreurs.isEmpty();
    }
}
