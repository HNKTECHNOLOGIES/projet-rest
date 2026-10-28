package fr.gouv.finances.rloc.async.batch.os55.processor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.rest.os55.enums.Os55ErreurEnum;
import fr.gouv.finances.rloc.async.service.os55.Os55BusinessService;

public class Os55ItemProcessor implements ItemProcessor<Os55DemandeDTO, Os55ProcessedItem> {

    private final ErreurDTOFactory erreurDTOFactory;
    private final Os55BusinessService businessService;

    public Os55ItemProcessor(ErreurDTOFactory erreurDTOFactory, Os55BusinessService businessService) {
        this.erreurDTOFactory = erreurDTOFactory;
        this.businessService = businessService;
    }

    @Override
    public Os55ProcessedItem process(Os55DemandeDTO item) {
        List<ErreurDTO> erreurs = new ArrayList<>();
        if (!StringUtils.hasText(item.getIdLocal())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.ID_LOCAL_ABSENT));
        }
        if (!StringUtils.hasText(item.getIdFiche())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.ID_FICHE_ABSENT));
        }
        if (!erreurs.isEmpty()) {
            return new Os55ProcessedItem(item, erreurs);
        }
        if (!businessService.isIdLocalFormatValid(item.getIdLocal())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.FORMAT_ID_LOCAL_INCORRECT, item.getIdLocal()));
        }
        if (!businessService.isIdFicheFormatValid(item.getIdFiche())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.FORMAT_ID_FICHE_INCORRECT, item.getIdFiche()));
        }
        if (!erreurs.isEmpty()) {
            return new Os55ProcessedItem(item, erreurs);
        }
        if (!businessService.existsLocal(item.getIdLocal())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.LOCAL_INEXISTANT, item.getIdLocal()));
            return new Os55ProcessedItem(item, erreurs);
        }
        if (!businessService.existsSurveillance(item.getIdLocal(), item.getIdFiche())) {
            erreurs.add(erreurDTOFactory.create(Os55ErreurEnum.COUPLE_SURVEILLANCE_INEXISTANT, item.getIdLocal(), item.getIdFiche()));
        }
        return new Os55ProcessedItem(item, erreurs);
    }
}
