package fr.gouv.finances.rloc.async.repository;

import java.util.Optional;

public interface Os55SurveillanceRepository {

    Optional<SuppressionSurveillanceResult> deleteSurveillanceForUpdate(String idLocal, String idFiche);

    record SuppressionSurveillanceResult(Long idSurveillance, int deletedRows) {
    }
}
