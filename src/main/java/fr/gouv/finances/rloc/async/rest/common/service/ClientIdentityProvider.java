package fr.gouv.finances.rloc.async.rest.common.service;

public interface ClientIdentityProvider {

    String resolveClientId(String clientIdHeader);
}
