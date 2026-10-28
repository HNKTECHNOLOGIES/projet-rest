package fr.gouv.finances.rloc.async.rest.os55.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Os55DemandeDTO {

    /** Numero unique de l'element au sein d'une demande multivaluee. */
    @JsonProperty(required = true)
    private Long num;
    private String idLocal;
    private String idFiche;
}
