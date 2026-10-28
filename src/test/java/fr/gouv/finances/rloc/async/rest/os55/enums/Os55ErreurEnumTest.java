package fr.gouv.finances.rloc.async.rest.os55.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Os55ErreurEnumTest {

    @ParameterizedTest
    @MethodSource("expectedErrors")
    void exposesActiveOs55BusinessErrors(Os55ErreurEnum erreur, int codeErreur, String libelleTypeErreur,
            int codeMotifRejet, String descriptifMotifRejet) {
        assertThat(erreur.getCodeErreur()).isEqualTo(codeErreur);
        assertThat(erreur.getLibelleTypeErreur()).isEqualTo(libelleTypeErreur);
        assertThat(erreur.getCodeMotifRejet()).isEqualTo(codeMotifRejet);
        assertThat(erreur.getDescriptifMotifRejet()).isEqualTo(descriptifMotifRejet);
    }

    @ParameterizedTest
    @MethodSource("activeErrors")
    void doesNotExposeCancelledMotif6020(Os55ErreurEnum erreur) {
        assertThat(erreur.getCodeMotifRejet()).isNotEqualTo(6020);
    }

    static Stream<Os55ErreurEnum> activeErrors() {
        return Stream.of(Os55ErreurEnum.values());
    }

    static Stream<Arguments> expectedErrors() {
        return Stream.of(
                Arguments.of(Os55ErreurEnum.ID_LOCAL_ABSENT, 50, "Erreur de présence", 5114,
                        "Identifiant du local absent."),
                Arguments.of(Os55ErreurEnum.ID_FICHE_ABSENT, 50, "Erreur de présence", 5173,
                        "Identifiant de la fiche de surveillance absent."),
                Arguments.of(Os55ErreurEnum.FORMAT_ID_LOCAL_INCORRECT, 10, "Erreur de format", 1232,
                        "Format de l'identifiant du local incorrect."),
                Arguments.of(Os55ErreurEnum.FORMAT_ID_FICHE_INCORRECT, 10, "Erreur de format", 1330,
                        "Format de l'identifiant de la fiche incorrect."),
                Arguments.of(Os55ErreurEnum.LOCAL_INEXISTANT, 60, "Erreur d'existence", 6001,
                        "Le local n'existe pas."),
                Arguments.of(Os55ErreurEnum.COUPLE_SURVEILLANCE_INEXISTANT, 60, "Erreur d'existence", 6023,
                        "Le couple idlocal-idfiche n'a pas de données de surveillance."));
    }
}
