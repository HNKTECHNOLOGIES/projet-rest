package fr.gouv.finances.rloc.async.rest.os55.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.finances.rloc.async.rest.common.dto.ErreurDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Os55ReponseDTO {

    /** Numero unique recopie depuis l'element de demande. */
    @JsonProperty(required = true)
    private Long num;
    /** Valeur attendue par la specification : OK ou KO. */
    private String codeRetour;
    private Long idSurveillance;
    private String idLocal;
    private String idFiche;
    /** Les codes motifs OS55 sont portes selon la convention existante de ErreurDTO. */
    private List<ErreurDTO> listeErreur;
}
