package fr.gouv.finances.rloc.async.common.error;

public interface ErreurMetier {

    int getCodeErreur();

    String getLibelleTypeErreur();

    int getCodeMotifRejet();

    String getDescriptifMotifRejet();
}
