package fr.gouv.finances.rloc.async.rest.common.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import fr.gouv.finances.rloc.async.exception.BadRequestException;

@Component
public class HeaderClientIdentityProvider implements ClientIdentityProvider {

    @Override
    public String resolveClientId(String clientIdHeader) {
        if (!StringUtils.hasText(clientIdHeader)) {
            throw new BadRequestException("X-Client-Id est obligatoire");
        }
        // Socle temporaire : a remplacer par le principal authentifie des que la securite est branchee.
        return clientIdHeader.trim();
    }
}
