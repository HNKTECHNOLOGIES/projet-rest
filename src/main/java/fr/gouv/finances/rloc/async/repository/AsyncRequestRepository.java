package fr.gouv.finances.rloc.async.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;

public interface AsyncRequestRepository extends JpaRepository<AsyncRequestEntity, Long> {

    Optional<AsyncRequestEntity> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Optional<AsyncRequestEntity> findFirstByCodeOSAndStatusOrderByDateCreationAsc(int codeOS, AsyncRequestStatus status);

    List<AsyncRequestEntity> findByStatusAndDateMiseAJourBefore(AsyncRequestStatus status, LocalDateTime dateMiseAJour);

    @Modifying
    @Query("""
            update AsyncRequestEntity request
               set request.status = fr.gouv.finances.rloc.async.entity.AsyncRequestStatus.EN_COURS,
                   request.dateDebut = :dateDebut
             where request.id = :id
               and request.status = fr.gouv.finances.rloc.async.entity.AsyncRequestStatus.EN_ATTENTE
            """)
    int markAsRunning(@Param("id") Long id, @Param("dateDebut") LocalDateTime dateDebut);
}
