package fr.gouv.finances.rloc.async.os55;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.batch.os55.writer.Os55ItemWriter;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.AsyncResultatElementRepository;
import fr.gouv.finances.rloc.async.repository.Os55SurveillanceRepository;
import fr.gouv.finances.rloc.async.rest.common.service.AsyncRequestService;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55ReponseDTO;
import fr.gouv.finances.rloc.async.service.os55.Os55BusinessService;
import fr.gouv.finances.rloc.async.service.os55.Os55ResultService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class Os55PostgresqlTestcontainersIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("rloc_async_test")
            .withUsername("rloc_test")
            .withPassword("rloc_test_password");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Os55SurveillanceRepository surveillanceRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AsyncRequestRepository requestRepository;
    @Autowired
    private AsyncResultatElementRepository resultatRepository;
    @Autowired
    private Os55ResultService resultService;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/test-migration");
        registry.add("rloc.storage.incoming-dir", () -> STORAGE_ROOT.resolve("incoming").toString());
        registry.add("rloc.storage.processing-dir", () -> STORAGE_ROOT.resolve("processing").toString());
        registry.add("rloc.storage.outgoing-dir", () -> STORAGE_ROOT.resolve("outgoing").toString());
        registry.add("rloc.storage.archive-dir", () -> STORAGE_ROOT.resolve("archive").toString());
    }

    @Test
    void initializesBusinessAndTechnicalTables() {
        Integer localCount = jdbcTemplate.queryForObject("select count(*) from local", Integer.class);
        Integer surveillanceCount = jdbcTemplate.queryForObject("select count(*) from surveillance", Integer.class);
        Integer resultUniqueCount = jdbcTemplate.queryForObject("""
                select count(*)
                  from information_schema.table_constraints
                 where table_name = 'async_resultat_element'
                   and constraint_name = 'uk_async_resultat_element_requete_num'
                """, Integer.class);

        assertThat(localCount).isGreaterThanOrEqualTo(5);
        assertThat(surveillanceCount).isGreaterThanOrEqualTo(5);
        assertThat(resultUniqueCount).isEqualTo(1);
    }

    @Test
    void findsAndDeletesOnlyRequestedSurveillance() {
        String local = unique("LOCAL_DELETE");
        insertLocal(local);
        insertSurveillance(local, "FICHE_A");
        insertSurveillance(local, "FICHE_B");

        Optional<Os55SurveillanceRepository.SuppressionSurveillanceResult> result = surveillanceRepository
                .deleteSurveillanceForUpdate(local, "FICHE_A");

        assertThat(result).isPresent();
        assertThat(result.get().deletedRows()).isEqualTo(1);
        assertThat(countSurveillance(local, "FICHE_A")).isZero();
        assertThat(countSurveillance(local, "FICHE_B")).isEqualTo(1);
    }

    @Test
    void rollsBackDeletionWhenTransactionFails() {
        String local = unique("LOCAL_ROLLBACK");
        insertLocal(local);
        insertSurveillance(local, "FICHE_ROLLBACK");
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            surveillanceRepository.deleteSurveillanceForUpdate(local, "FICHE_ROLLBACK");
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countSurveillance(local, "FICHE_ROLLBACK")).isEqualTo(1);
    }

    @Test
    void deleteAndResultPersistInSameTransaction() {
        String local = unique("LOCAL_RESULT");
        insertLocal(local);
        insertSurveillance(local, "FICHE_RESULT");
        AsyncRequestEntity request = saveRequest(1L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            surveillanceRepository.deleteSurveillanceForUpdate(local, "FICHE_RESULT");
            resultService.saveResult(request.getId(), new Os55ReponseDTO(1L, "OK", 1L, local, "FICHE_RESULT", null));
        });

        assertThat(countSurveillance(local, "FICHE_RESULT")).isZero();
        assertThat(resultatRepository.countByRequeteId(request.getId())).isEqualTo(1L);
    }

    @Test
    void rollsBackDeleteAndResultTogether() {
        String local = unique("LOCAL_RESULT_ROLLBACK");
        insertLocal(local);
        insertSurveillance(local, "FICHE_RESULT_ROLLBACK");
        AsyncRequestEntity request = saveRequest(1L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            surveillanceRepository.deleteSurveillanceForUpdate(local, "FICHE_RESULT_ROLLBACK");
            resultService.saveResult(request.getId(), new Os55ReponseDTO(1L, "OK", 1L, local,
                    "FICHE_RESULT_ROLLBACK", null));
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countSurveillance(local, "FICHE_RESULT_ROLLBACK")).isEqualTo(1);
        assertThat(resultatRepository.countByRequeteId(request.getId())).isZero();
    }

    @Test
    void persistedResultPreventsDoubleDeletionOnRestart() throws Exception {
        String local = unique("LOCAL_RESTART");
        insertLocal(local);
        insertSurveillance(local, "FICHE_RESTART");
        AsyncRequestEntity request = saveRequest(1L);
        resultService.saveResult(request.getId(), new Os55ReponseDTO(1L, "OK", 77L, local, "FICHE_RESTART", null));
        Os55ItemWriter writer = new Os55ItemWriter(request.getUuid(), requestRepository, surveillanceRepository,
                resultService, new ErreurDTOFactory());

        writer.write(new Chunk<>(new Os55ProcessedItem(new Os55DemandeDTO(1L, local, "FICHE_RESTART"), java.util.List.of())));

        assertThat(countSurveillance(local, "FICHE_RESTART")).isEqualTo(1);
        assertThat(resultatRepository.countByRequeteId(request.getId())).isEqualTo(1L);
    }

    private AsyncRequestEntity saveRequest(Long declaredCount) {
        AsyncRequestEntity request = new AsyncRequestEntity();
        request.setUuid(UUID.randomUUID());
        request.setClientId("test-client");
        request.setCodeOS(AsyncRequestService.OS55_CODE);
        request.setStatus(AsyncRequestStatus.EN_COURS);
        request.setDeclaredElementCount(declaredCount);
        request.setProcessedElementCount(0L);
        return requestRepository.saveAndFlush(request);
    }

    private void insertLocal(String idLocal) {
        jdbcTemplate.update("insert into local(id_local) values (?)", idLocal);
    }

    private void insertSurveillance(String idLocal, String idFiche) {
        jdbcTemplate.update("insert into surveillance(id_local, id_fiche) values (?, ?)", idLocal, idFiche);
    }

    private int countSurveillance(String idLocal, String idFiche) {
        return jdbcTemplate.queryForObject("select count(*) from surveillance where id_local = ? and id_fiche = ?",
                Integer.class, idLocal, idFiche);
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("rloc-async-pg-test-");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class TestOs55BusinessConfiguration {

        @Bean
        Os55SurveillanceRepository testSurveillanceRepository(JdbcTemplate jdbcTemplate) {
            return (idLocal, idFiche) -> {
                java.util.List<Long> ids = jdbcTemplate.query("""
                        select id_surveillance
                          from surveillance
                         where id_local = ?
                           and id_fiche = ?
                         for update
                        """, (rs, rowNum) -> rs.getLong(1), idLocal, idFiche);
                if (ids.isEmpty()) {
                    return Optional.empty();
                }
                Long idSurveillance = ids.get(0);
                int deleted = jdbcTemplate.update("delete from surveillance where id_surveillance = ?", idSurveillance);
                return Optional.of(new Os55SurveillanceRepository.SuppressionSurveillanceResult(idSurveillance, deleted));
            };
        }

        @Bean
        Os55BusinessService testBusinessService(JdbcTemplate jdbcTemplate) {
            return new Os55BusinessService() {
                @Override
                public boolean isIdLocalFormatValid(String idLocal) {
                    return true;
                }

                @Override
                public boolean isIdFicheFormatValid(String idFiche) {
                    return true;
                }

                @Override
                public boolean existsLocal(String idLocal) {
                    Integer count = jdbcTemplate.queryForObject("select count(*) from local where id_local = ?",
                            Integer.class, idLocal);
                    return count != null && count > 0;
                }

                @Override
                public boolean existsSurveillance(String idLocal, String idFiche) {
                    Integer count = jdbcTemplate.queryForObject(
                            "select count(*) from surveillance where id_local = ? and id_fiche = ?", Integer.class,
                            idLocal, idFiche);
                    return count != null && count > 0;
                }
            };
        }
    }
}
