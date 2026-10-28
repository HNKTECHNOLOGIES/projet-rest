package fr.gouv.finances.rloc.async.rest.common.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErreurDTO {

    private Integer code;
    private String descriptif;
    /** Identifiant technique pouvant contenir une valeur encodee en Base64. */
    private String id;
    private String libelle;
    private String message;
    private Integer severite;
    private Instant timestamp;
    private Integer codeMotifRejet;
    private String descriptifMotifRejet;

    public ErreurDTO(Integer code, String descriptif, String id, String libelle, String message, Integer severite,
            Instant timestamp) {
        this.code = code;
        this.descriptif = descriptif;
        this.id = id;
        this.libelle = libelle;
        this.message = message;
        this.severite = severite;
        this.timestamp = timestamp;
    }
}
