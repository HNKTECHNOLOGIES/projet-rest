package fr.gouv.finances.rloc.async.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "async_resultat_element", uniqueConstraints = @UniqueConstraint(name = "uk_async_resultat_element_requete_num", columnNames = {
        "requete_id", "num" }))
public class AsyncResultatElementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requete_id", nullable = false)
    private Long requeteId;

    @Column(nullable = false)
    private Long num;

    @Column(name = "code_retour", nullable = false, length = 8)
    private String codeRetour;

    @Column(name = "resultat_json", nullable = false, columnDefinition = "TEXT")
    private String resultatJson;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void prePersist() {
        dateCreation = LocalDateTime.now();
    }
}
