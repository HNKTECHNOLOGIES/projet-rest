package fr.gouv.finances.rloc.async.common.error;

import java.time.Instant;

import org.springframework.stereotype.Component;

import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;

@Component
public class ErreurDTOFactory {

    private static final String VALEUR_PLACEHOLDER = "%valeur";

    public ErreurDTO create(ErreurMetier erreurMetier, Object... valeurs) {
        String descriptifMotifRejet = replaceValeurs(erreurMetier.getDescriptifMotifRejet(), valeurs);
        ErreurDTO dto = new ErreurDTO();
        dto.setCode(erreurMetier.getCodeErreur());
        dto.setLibelle(erreurMetier.getLibelleTypeErreur());
        dto.setDescriptif(descriptifMotifRejet);
        dto.setCodeMotifRejet(erreurMetier.getCodeMotifRejet());
        dto.setDescriptifMotifRejet(descriptifMotifRejet);
        dto.setTimestamp(Instant.now());
        return dto;
    }

    String replaceValeurs(String template, Object... valeurs) {
        if (template == null) {
            return null;
        }
        String result = template;
        int valueIndex = 0;
        while (result.contains(VALEUR_PLACEHOLDER)) {
            String replacement = "";
            if (valeurs != null && valueIndex < valeurs.length && valeurs[valueIndex] != null) {
                replacement = String.valueOf(valeurs[valueIndex]);
            }
            result = result.replaceFirst(VALEUR_PLACEHOLDER, java.util.regex.Matcher.quoteReplacement(replacement));
            valueIndex++;
        }
        return result;
    }
}
