package fr.gouv.finances.rloc.async.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.os55.enums.Os55ErreurEnum;

class ErreurDTOFactoryTest {

    private final ErreurDTOFactory factory = new ErreurDTOFactory();
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void createsErreurDtoFromOs55ErreurMetier() {
        ErreurDTO erreur = factory.create(Os55ErreurEnum.ID_LOCAL_ABSENT);

        assertThat(erreur.getCode()).isEqualTo(50);
        assertThat(erreur.getLibelle()).isEqualTo("Erreur de présence");
        assertThat(erreur.getCodeMotifRejet()).isEqualTo(5114);
        assertThat(erreur.getDescriptifMotifRejet()).isEqualTo("Identifiant du local absent.");
        assertThat(erreur.getDescriptif()).isEqualTo("Identifiant du local absent.");
        assertThat(erreur.getTimestamp()).isNotNull();
        assertThat(erreur.getId()).isNull();
        assertThat(erreur.getMessage()).isNull();
        assertThat(erreur.getSeverite()).isNull();
    }

    @Test
    void replacesValeurPlaceholdersWithProvidedValues() {
        ErreurDTO erreur = factory.create(TestErreurMetier.VALEUR_INVALIDE, "LOCAL-1", "FICHE-1");

        assertThat(erreur.getDescriptifMotifRejet()).isEqualTo("Valeur LOCAL-1 invalide pour FICHE-1.");
        assertThat(erreur.getDescriptifMotifRejet()).doesNotContain("%valeur");
    }

    @Test
    void removesValeurPlaceholderWhenValueIsMissing() {
        ErreurDTO erreur = factory.create(TestErreurMetier.VALEUR_INVALIDE, "LOCAL-1");

        assertThat(erreur.getDescriptifMotifRejet()).isEqualTo("Valeur LOCAL-1 invalide pour .");
        assertThat(erreur.getDescriptifMotifRejet()).doesNotContain("%valeur");
    }

    @Test
    void serializesErreurDtoWithExistingAndMotifFields() throws Exception {
        ErreurDTO erreur = factory.create(Os55ErreurEnum.COUPLE_SURVEILLANCE_INEXISTANT);

        String json = objectMapper.writeValueAsString(erreur);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("code").asInt()).isEqualTo(60);
        assertThat(node.get("libelle").asText()).isEqualTo("Erreur d'existence");
        assertThat(node.get("codeMotifRejet").asInt()).isEqualTo(6023);
        assertThat(node.get("descriptifMotifRejet").asText())
                .isEqualTo("Le couple idlocal-idfiche n'a pas de données de surveillance.");
        assertThat(node.has("severite")).isFalse();
    }

    @Test
    void preservesExistingErreurDtoConstructor() {
        Instant timestamp = Instant.parse("2025-05-20T10:15:30Z");
        ErreurDTO erreur = new ErreurDTO(99, "descriptif", "id", "libelle", "message", 1, timestamp);

        assertThat(erreur.getCode()).isEqualTo(99);
        assertThat(erreur.getDescriptif()).isEqualTo("descriptif");
        assertThat(erreur.getId()).isEqualTo("id");
        assertThat(erreur.getLibelle()).isEqualTo("libelle");
        assertThat(erreur.getMessage()).isEqualTo("message");
        assertThat(erreur.getSeverite()).isEqualTo(1);
        assertThat(erreur.getTimestamp()).isEqualTo(timestamp);
        assertThat(erreur.getCodeMotifRejet()).isNull();
        assertThat(erreur.getDescriptifMotifRejet()).isNull();
    }

    private enum TestErreurMetier implements ErreurMetier {
        VALEUR_INVALIDE(70, "Erreur de test", 7001, "Valeur %valeur invalide pour %valeur.");

        private final int codeErreur;
        private final String libelleTypeErreur;
        private final int codeMotifRejet;
        private final String descriptifMotifRejet;

        TestErreurMetier(int codeErreur, String libelleTypeErreur, int codeMotifRejet,
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
}
