package fr.gouv.finances.rloc.async.repository;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.gouv.finances.rloc.async.entity.AsyncResultatElementEntity;

public interface AsyncResultatElementRepository extends JpaRepository<AsyncResultatElementEntity, Long> {

    boolean existsByRequeteIdAndNum(Long requeteId, Long num);

    long countByRequeteId(Long requeteId);

    List<AsyncResultatElementEntity> findByRequeteIdOrderByNumAsc(Long requeteId);

    @Query("select r from AsyncResultatElementEntity r where r.requeteId = :requeteId order by r.num asc")
    Stream<AsyncResultatElementEntity> streamByRequeteIdOrderByNumAsc(@Param("requeteId") Long requeteId);
}
