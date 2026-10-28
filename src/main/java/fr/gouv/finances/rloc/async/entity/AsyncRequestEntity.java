package fr.gouv.finances.rloc.async.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "async_request", uniqueConstraints = @UniqueConstraint(name = "uk_async_request_uuid", columnNames = "uuid"))
public class AsyncRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "code_os", nullable = false)
    private int codeOS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AsyncRequestStatus status;

    @Column(name = "input_file_path", length = 1024)
    private String inputFilePath;

    @Column(name = "output_file_path", length = 1024)
    private String outputFilePath;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "declared_element_count")
    private Long declaredElementCount;

    @Column(name = "processed_element_count", nullable = false)
    private long processedElementCount;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        dateCreation = now;
        dateMiseAJour = now;
    }

    @PreUpdate
    void preUpdate() {
        dateMiseAJour = LocalDateTime.now();
    }
}
