package fr.gouv.finances.rloc.async.service.os55;

public interface Os55BusinessService {

    boolean isIdLocalFormatValid(String idLocal);

    boolean isIdFicheFormatValid(String idFiche);

    boolean existsLocal(String idLocal);

    boolean existsSurveillance(String idLocal, String idFiche);
}
