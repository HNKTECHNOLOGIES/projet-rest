package fr.gouv.finances.rloc.async.rest.os55.enums;

import fr.gouv.finances.rloc.async.common.error.ErreurMetier;

public enum Os55ErreurEnum implements ErreurMetier {

    ID_LOCAL_ABSENT(50, "Erreur de présence", 5114, "Identifiant du local absent."),
    ID_FICHE_ABSENT(50, "Erreur de présence", 5173,
            "Identifiant de la fiche de surveillance absent."),
    FORMAT_ID_LOCAL_INCORRECT(10, "Erreur de format", 1232,
            "Format de l'identifiant du local incorrect."),
    FORMAT_ID_FICHE_INCORRECT(10, "Erreur de format", 1330,
            "Format de l'identifiant de la fiche incorrect."),
    LOCAL_INEXISTANT(60, "Erreur d'existence", 6001, "Le local n'existe pas."),
    COUPLE_SURVEILLANCE_INEXISTANT(60, "Erreur d'existence", 6023,
            "Le couple idlocal-idfiche n'a pas de données de surveillance.");

    private final int codeErreur;
    private final String libelleTypeErreur;
    private final int codeMotifRejet;
    private final String descriptifMotifRejet;

    Os55ErreurEnum(int codeErreur, String libelleTypeErreur, int codeMotifRejet,
            String descriptifMotifRejet) {
        this.codeErreur = codeErreur;
        this.libelleTypeErreur = libelleTypeErreur;
        this.codeMotifRejet = codeMotifRejet;
        this.descriptifMotifRejet = descriptifMotifRejet;
    }

    @Override
    public int getCodeErreur() {
        return codeErreur;
    }

    @Override
    public String getLibelleTypeErreur() {
        return libelleTypeErreur;
    }

    @Override
    public int getCodeMotifRejet() {
        return codeMotifRejet;
    }

    @Override
    public String getDescriptifMotifRejet() {
        return descriptifMotifRejet;
    }
}
