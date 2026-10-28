package fr.gouv.finances.rloc.async.rest.common.service;

import org.springframework.stereotype.Service;

import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.rest.common.dto.RequeteDTO;

@Service
public class RequestStatusService {

    public RequeteDTO toPublicDto(AsyncRequestEntity request) {
        RequeteDTO dto = new RequeteDTO();
        dto.setId(request.getId());
        dto.setIeis(request.getClientId());
        dto.setStatus(request.getStatus().name());
        dto.setCodeOS(request.getCodeOS());
        dto.setDateCreation(toJoda(request.getDateCreation()));
        dto.setDateMiseAJour(toJoda(request.getDateMiseAJour()));
        dto.setDateDebut(toJoda(request.getDateDebut()));
        dto.setDateFin(toJoda(request.getDateFin()));
        return dto;
    }

    private org.joda.time.LocalDateTime toJoda(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return new org.joda.time.LocalDateTime(value.getYear(), value.getMonthValue(), value.getDayOfMonth(),
                value.getHour(), value.getMinute(), value.getSecond(), value.getNano() / 1_000_000);
    }
}
