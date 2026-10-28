package fr.gouv.finances.rloc.async.rest.os55.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Os55AsyncControllerIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AsyncRequestRepository repository;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("rloc.storage.incoming-dir", () -> STORAGE_ROOT.resolve("incoming").toString());
        registry.add("rloc.storage.processing-dir", () -> STORAGE_ROOT.resolve("processing").toString());
        registry.add("rloc.storage.outgoing-dir", () -> STORAGE_ROOT.resolve("outgoing").toString());
        registry.add("rloc.storage.archive-dir", () -> STORAGE_ROOT.resolve("archive").toString());
        registry.add("rloc.async-request.max-size-bytes", () -> "512");
        registry.add("rloc.async-request.max-element-count", () -> "10");
    }

    @BeforeEach
    void cleanDatabase() throws IOException {
        repository.deleteAll();
        cleanDirectory(STORAGE_ROOT.resolve("incoming"));
        cleanDirectory(STORAGE_ROOT.resolve("processing"));
        cleanDirectory(STORAGE_ROOT.resolve("outgoing"));
        cleanDirectory(STORAGE_ROOT.resolve("archive"));
    }

    @AfterAll
    static void cleanStorageRoot() throws IOException {
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void submitValidRequestReturnsAccepted() throws Exception {
        UUID uuid = UUID.randomUUID();

        mockMvc.perform(post("/api/os55")
                .contentType(Os55AsyncController.NDJSON_VALUE)
                .header("X-Request-Id", uuid)
                .header("X-Client-Id", "client-a")
                .header("X-Element-Count", "2")
                .content(validTwoLineBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.uuid").value(uuid.toString()));

        AsyncRequestEntity request = repository.findByUuid(uuid).orElseThrow();
        assertThat(request.getStatus()).isEqualTo(AsyncRequestStatus.EN_ATTENTE);
        assertThat(request.getDeclaredElementCount()).isEqualTo(2L);
        assertThat(Files.exists(Path.of(request.getInputFilePath()))).isTrue();
    }

    @Test
    void submitExistingUuidReturnsConflict() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");

        mockMvc.perform(post("/api/os55")
                .contentType(Os55AsyncController.NDJSON_VALUE)
                .header("X-Request-Id", uuid)
                .header("X-Client-Id", "client-a")
                .header("X-Element-Count", "1")
                .content(validOneLineBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void submitMissingMetadataReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/os55")
                .contentType(Os55AsyncController.NDJSON_VALUE)
                .header("X-Client-Id", "client-a")
                .header("X-Element-Count", "1")
                .content(validOneLineBody()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitTooLargeUploadReturnsPayloadTooLarge() throws Exception {
        UUID uuid = UUID.randomUUID();
        String largeBody = "{\"num\":1,\"idLocal\":\"" + "1".repeat(600) + "\",\"idFiche\":\"1001\"}";

        mockMvc.perform(post("/api/os55")
                .contentType(Os55AsyncController.NDJSON_VALUE)
                .header("X-Request-Id", uuid)
                .header("X-Client-Id", "client-a")
                .header("X-Element-Count", "1")
                .content(largeBody))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void statusUnknownRequestReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/os55/{uuid}/status", UUID.randomUUID())
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusForOtherClientReturnsForbidden() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");

        mockMvc.perform(get("/api/os55/{uuid}/status", uuid)
                .header("X-Client-Id", "client-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    void statusReturnsPendingRequest() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");

        mockMvc.perform(get("/api/os55/{uuid}/status", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.codeOS").value(55))
                .andExpect(jsonPath("$.cheminFichierEntree").doesNotExist())
                .andExpect(jsonPath("$.cheminFichierSortie").doesNotExist());
    }

    @Test
    void statusReturnsRunningRequest() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");
        AsyncRequestEntity request = repository.findByUuid(uuid).orElseThrow();
        request.setStatus(AsyncRequestStatus.EN_COURS);
        repository.saveAndFlush(request);

        mockMvc.perform(get("/api/os55/{uuid}/status", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"));
    }

    @Test
    void downloadsFinishedRequestAsNdjsonStream() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");
        String output = """
                {"num":1,"codeRetour":"OK","idSurveillance":123}
                """;
        markFinishedWithOutput(uuid, output);

        mockMvc.perform(get("/api/os55/{uuid}", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(Os55AsyncController.NDJSON_VALUE))
                .andExpect(content().string(output));
    }

    @Test
    void downloadBeforeCompletionReturnsConflict() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");

        mockMvc.perform(get("/api/os55/{uuid}", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isConflict());
    }

    @Test
    void acknowledgeIsIdempotent() throws Exception {
        UUID uuid = submitOneLineRequest("client-a");
        Path outputPath = markFinishedWithOutput(uuid, """
                {"num":1,"codeRetour":"OK"}
                """);

        mockMvc.perform(post("/api/os55/{uuid}/ack", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACQUITTE"));

        mockMvc.perform(post("/api/os55/{uuid}/ack", uuid)
                .header("X-Client-Id", "client-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACQUITTE"));

        AsyncRequestEntity request = repository.findByUuid(uuid).orElseThrow();
        assertThat(request.getStatus()).isEqualTo(AsyncRequestStatus.ACQUITTE);
        assertThat(Files.exists(outputPath)).isFalse();
    }

    @Test
    void concurrentSubmissionsWithSameUuidCreateOnlyOneRequest() throws Exception {
        UUID uuid = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<Integer> call = () -> {
            start.await();
            return mockMvc.perform(post("/api/os55")
                    .contentType(Os55AsyncController.NDJSON_VALUE)
                    .header("X-Request-Id", uuid)
                    .header("X-Client-Id", "client-a")
                    .header("X-Element-Count", "1")
                    .content(validOneLineBody()))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Future<Integer> first = executorService.submit(call);
        Future<Integer> second = executorService.submit(call);
        start.countDown();
        List<Integer> statuses = List.of(first.get(), second.get());
        executorService.shutdownNow();

        assertThat(statuses).contains(202, 409);
        assertThat(repository.findAll()).hasSize(1);
    }

    private UUID submitOneLineRequest(String clientId) throws Exception {
        UUID uuid = UUID.randomUUID();
        mockMvc.perform(post("/api/os55")
                .contentType(Os55AsyncController.NDJSON_VALUE)
                .header("X-Request-Id", uuid)
                .header("X-Client-Id", clientId)
                .header("X-Element-Count", "1")
                .content(validOneLineBody()))
                .andExpect(status().isAccepted());
        return uuid;
    }

    private Path markFinishedWithOutput(UUID uuid, String output) throws IOException {
        Path outputPath = STORAGE_ROOT.resolve("outgoing").resolve(uuid + "-result.ndjson");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, output, StandardCharsets.UTF_8);
        AsyncRequestEntity request = repository.findByUuid(uuid).orElseThrow();
        request.setStatus(AsyncRequestStatus.TERMINE);
        request.setOutputFilePath(outputPath.toString());
        repository.saveAndFlush(request);
        return outputPath;
    }

    private static String validOneLineBody() {
        return """
                {"num":1,"idLocal":"123456","idFiche":"1001"}
                """;
    }

    private static String validTwoLineBody() {
        return validOneLineBody() + """
                {"num":2,"idLocal":"789012","idFiche":"1002"}
                """;
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("rloc-async-test-");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void cleanDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var paths = Files.list(directory)) {
            paths.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        try (var nested = Files.walk(path)) {
                            nested.sorted(Comparator.reverseOrder()).forEach(nestedPath -> {
                                try {
                                    Files.deleteIfExists(nestedPath);
                                } catch (IOException ignored) {
                                }
                            });
                        }
                    } else {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException ignored) {
                }
            });
        }
    }
}
