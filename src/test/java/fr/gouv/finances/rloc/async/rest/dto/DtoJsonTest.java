package fr.gouv.finances.rloc.async.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import fr.gouv.finances.rloc.async.rest.common.dto.RequeteDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55ReponseDTO;

class DtoJsonTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addModule(new JodaModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void serializesAndDeserializesErreurDto() throws Exception {
        ErreurDTO erreur = new ErreurDTO(50, "idLocal absent", "NTExNA==", "5114", "Champ obligatoire", null,
                Instant.parse("2025-05-20T10:15:30Z"));

        String json = objectMapper.writeValueAsString(erreur);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("code").asInt()).isEqualTo(50);
        assertThat(node.get("id").asText()).isEqualTo("NTExNA==");
        assertThat(node.has("severite")).isFalse();

        ErreurDTO deserialized = objectMapper.readValue(json, ErreurDTO.class);
        assertThat(deserialized.getCode()).isEqualTo(50);
        assertThat(deserialized.getTimestamp()).isEqualTo(Instant.parse("2025-05-20T10:15:30Z"));
    }

    @Test
    void excludesNullProperties() throws Exception {
        Os55ReponseDTO reponse = new Os55ReponseDTO();
        reponse.setNum(1L);
        reponse.setCodeRetour("OK");

        String json = objectMapper.writeValueAsString(reponse);

        assertThat(json).contains("\"num\":1");
        assertThat(json).contains("\"codeRetour\":\"OK\"");
        assertThat(json).doesNotContain("idSurveillance");
        assertThat(json).doesNotContain("listeErreur");
    }

    @Test
    void preservesNumWhenSerializingAndDeserializingOs55Demande() throws Exception {
        String json = """
                {"num":42,"idLocal":"LOCAL-1","idFiche":"FICHE-1"}
                """;

        Os55DemandeDTO demande = objectMapper.readValue(json, Os55DemandeDTO.class);
        String serialized = objectMapper.writeValueAsString(demande);
        JsonNode node = objectMapper.readTree(serialized);

        assertThat(demande.getNum()).isEqualTo(42L);
        assertThat(node.get("num").asLong()).isEqualTo(42L);
        assertThat(serialized).doesNotContain("elementId");
    }

    @Test
    void serializesOs55ResponseWithErrorList() throws Exception {
        ErreurDTO erreur = new ErreurDTO(60, "Couple sans surveillance", "NjAyMw==", "6023", "Aucune surveillance", null,
                Instant.parse("2025-05-20T11:00:00Z"));
        Os55ReponseDTO reponse = new Os55ReponseDTO(7L, "KO", null, "LOCAL-1", "FICHE-1", List.of(erreur));

        String json = objectMapper.writeValueAsString(reponse);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("num").asLong()).isEqualTo(7L);
        assertThat(node.get("codeRetour").asText()).isEqualTo("KO");
        assertThat(node.get("listeErreur")).hasSize(1);
        assertThat(node.get("listeErreur").get(0).get("code").asInt()).isEqualTo(60);
        assertThat(node.get("listeErreur").get(0).get("libelle").asText()).isEqualTo("6023");
        assertThat(node.has("idSurveillance")).isFalse();
    }

    @Test
    void serializesAndDeserializesRequeteDtoDates() throws Exception {
        LocalDateTime dateCreation = new LocalDateTime(2025, 5, 20, 14, 30, 15);
        RequeteDTO requete = new RequeteDTO();
        requete.setId(100L);
        requete.setIeis("IEIS-1");
        requete.setStatus("EN_ATTENTE");
        requete.setCodeOS(55);
        requete.setDateCreation(dateCreation);

        String json = objectMapper.writeValueAsString(requete);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("dateCreation").asText()).startsWith("2025-05-20T14:30:15");
        assertThat(node.has("cheminFichierEntree")).isFalse();
        assertThat(node.has("cheminFichierSortie")).isFalse();

        RequeteDTO deserialized = objectMapper.readValue(json, RequeteDTO.class);
        assertThat(deserialized.getDateCreation()).isEqualTo(dateCreation);
        assertThat(deserialized.getCodeOS()).isEqualTo(55);
    }
}
