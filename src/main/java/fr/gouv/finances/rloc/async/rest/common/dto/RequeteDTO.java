package fr.gouv.finances.rloc.async.rest.common.dto;

import java.util.List;

import org.joda.time.LocalDateTime;

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
public class RequeteDTO {

    private Long id;
    private String ieis;
    private String status;
    private int codeOS;
    /** Chemin interne : ne doit pas etre expose tel quel par une API REST publique. */
    private String cheminFichierEntree;
    /** Chemin interne : ne doit pas etre expose tel quel par une API REST publique. */
    private String cheminFichierSortie;
    private LocalDateTime dateCreation;
    private LocalDateTime dateMiseAJour;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private List<ErreurDTO> listeErreur;
}
