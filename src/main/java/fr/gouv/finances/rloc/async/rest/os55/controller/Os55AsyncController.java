package fr.gouv.finances.rloc.async.rest.os55.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.finances.rloc.async.rest.common.dto.RequeteDTO;
import fr.gouv.finances.rloc.async.rest.common.service.AsyncRequestService;
import fr.gouv.finances.rloc.async.rest.common.service.ClientIdentityProvider;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/os55")
public class Os55AsyncController {

    public static final String NDJSON_VALUE = "application/x-ndjson";
    public static final MediaType NDJSON = MediaType.valueOf(NDJSON_VALUE);

    private final AsyncRequestService asyncRequestService;
    private final ClientIdentityProvider clientIdentityProvider;

    public Os55AsyncController(AsyncRequestService asyncRequestService, ClientIdentityProvider clientIdentityProvider) {
        this.asyncRequestService = asyncRequestService;
        this.clientIdentityProvider = clientIdentityProvider;
    }

    @PostMapping(consumes = NDJSON_VALUE)
    public ResponseEntity<Map<String, String>> submit(@RequestHeader("X-Request-Id") UUID requestId,
            @RequestHeader("X-Client-Id") String clientIdHeader,
            @RequestHeader("X-Element-Count") Long elementCount,
            HttpServletRequest request) throws IOException {
        String clientId = clientIdentityProvider.resolveClientId(clientIdHeader);
        UUID uuid = asyncRequestService.submit(requestId, clientId, elementCount, request.getInputStream());
        return ResponseEntity.accepted().body(Map.of("uuid", uuid.toString()));
    }

    @GetMapping("/{uuid}/status")
    public ResponseEntity<RequeteDTO> status(@PathVariable UUID uuid,
            @RequestHeader("X-Client-Id") String clientIdHeader) {
        String clientId = clientIdentityProvider.resolveClientId(clientIdHeader);
        return ResponseEntity.ok(asyncRequestService.getStatus(uuid, clientId));
    }

    @GetMapping(value = "/{uuid}", produces = NDJSON_VALUE)
    public ResponseEntity<InputStreamResource> results(@PathVariable UUID uuid,
            @RequestHeader("X-Client-Id") String clientIdHeader) {
        String clientId = clientIdentityProvider.resolveClientId(clientIdHeader);
        InputStreamResource resource = asyncRequestService.openResults(uuid, clientId);
        return ResponseEntity.ok()
                .contentType(NDJSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uuid + "-result.ndjson\"")
                .body(resource);
    }

    @PostMapping("/{uuid}/ack")
    public ResponseEntity<Map<String, String>> acknowledge(@PathVariable UUID uuid,
            @RequestHeader("X-Client-Id") String clientIdHeader) {
        String clientId = clientIdentityProvider.resolveClientId(clientIdHeader);
        return ResponseEntity.ok(asyncRequestService.acknowledge(uuid, clientId));
    }
}
