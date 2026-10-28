package fr.gouv.finances.rloc.async.batch.os55.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.service.os55.Os55BusinessService;

class Os55ItemProcessorTest {

    @Test
    void rejectsMissingIdLocalWith5114() {
        Os55ProcessedItem result = processor(validBusiness()).process(new Os55DemandeDTO(1L, null, "1001"));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(5114);
    }

    @Test
    void rejectsMissingIdFicheWith5173() {
        Os55ProcessedItem result = processor(validBusiness()).process(new Os55DemandeDTO(1L, "123456", ""));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(5173);
    }

    @Test
    void rejectsInvalidIdLocalFormatWith1232() {
        Os55ProcessedItem result = processor(new FakeBusiness(false, true, true, true))
                .process(new Os55DemandeDTO(1L, "bad", "1001"));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(1232);
    }

    @Test
    void rejectsInvalidIdFicheFormatWith1330() {
        Os55ProcessedItem result = processor(new FakeBusiness(true, false, true, true))
                .process(new Os55DemandeDTO(1L, "123456", "bad"));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(1330);
    }

    @Test
    void rejectsMissingLocalWith6001() {
        Os55ProcessedItem result = processor(new FakeBusiness(true, true, false, true))
                .process(new Os55DemandeDTO(1L, "123456", "1001"));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(6001);
    }

    @Test
    void rejectsMissingSurveillanceWith6023() {
        Os55ProcessedItem result = processor(new FakeBusiness(true, true, true, false))
                .process(new Os55DemandeDTO(1L, "123456", "1001"));
        assertThat(result.erreurs()).extracting("codeMotifRejet").containsExactly(6023);
    }

    @Test
    void acceptsValidItem() {
        Os55ProcessedItem result = processor(validBusiness()).process(new Os55DemandeDTO(1L, "123456", "1001"));
        assertThat(result.erreurs()).isEmpty();
    }

    private Os55ItemProcessor processor(Os55BusinessService businessService) {
        return new Os55ItemProcessor(new ErreurDTOFactory(), businessService);
    }

    private Os55BusinessService validBusiness() {
        return new FakeBusiness(true, true, true, true);
    }

    private record FakeBusiness(boolean idLocalFormat, boolean idFicheFormat, boolean localExists,
            boolean surveillanceExists) implements Os55BusinessService {

        @Override
        public boolean isIdLocalFormatValid(String idLocal) {
            return idLocalFormat;
        }

        @Override
        public boolean isIdFicheFormatValid(String idFiche) {
            return idFicheFormat;
        }

        @Override
        public boolean existsLocal(String idLocal) {
            return localExists;
        }

        @Override
        public boolean existsSurveillance(String idLocal, String idFiche) {
            return surveillanceExists;
        }
    }
}
