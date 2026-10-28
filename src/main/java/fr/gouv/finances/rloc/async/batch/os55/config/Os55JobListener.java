package fr.gouv.finances.rloc.async.batch.os55.config;

import java.util.UUID;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.service.os55.Os55ResultService;

public class Os55JobListener implements JobExecutionListener {

    private final AsyncRequestRepository requestRepository;
    private final Os55ResultService resultService;

    public Os55JobListener(AsyncRequestRepository requestRepository, Os55ResultService resultService) {
        this.requestRepository = requestRepository;
        this.resultService = resultService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        UUID requestUuid = UUID.fromString(jobExecution.getJobParameters().getString("requestUuid"));
        AsyncRequestEntity request = requestRepository.findByUuid(requestUuid).orElseThrow();
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            resultService.publishResults(request);
            return;
        }
        request.setStatus(AsyncRequestStatus.ERREUR);
        requestRepository.saveAndFlush(request);
    }
}
