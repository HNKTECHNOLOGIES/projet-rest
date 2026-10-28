package fr.gouv.finances.rloc.async.batch.common;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.entity.AsyncRequestStatus;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.rest.common.service.AsyncRequestService;

@Service
@ConditionalOnBean(name = "os55Job")
public class RequestSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSchedulerService.class);

    private final AsyncRequestRepository requestRepository;
    private final JobLauncher jobLauncher;
    private final Job os55Job;

    public RequestSchedulerService(AsyncRequestRepository requestRepository, JobLauncher jobLauncher, Job os55Job) {
        this.requestRepository = requestRepository;
        this.jobLauncher = jobLauncher;
        this.os55Job = os55Job;
    }

    public Optional<AsyncRequestEntity> launchNextOs55Request() {
        Optional<AsyncRequestEntity> reserved = reserveNextOs55Request();
        reserved.ifPresent(this::launch);
        return reserved;
    }

    @Transactional
    protected Optional<AsyncRequestEntity> reserveNextOs55Request() {
        return requestRepository.findFirstByCodeOSAndStatusOrderByDateCreationAsc(AsyncRequestService.OS55_CODE,
                AsyncRequestStatus.EN_ATTENTE)
                .filter(request -> requestRepository.markAsRunning(request.getId(), LocalDateTime.now()) == 1)
                .flatMap(request -> requestRepository.findById(request.getId()));
    }

    private void launch(AsyncRequestEntity request) {
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addString("requestUuid", request.getUuid().toString(), true)
                    .addLong("requestId", request.getId(), true)
                    .toJobParameters();
            jobLauncher.run(os55Job, parameters);
        } catch (Exception exception) {
            LOGGER.error("Impossible de lancer le job OS55 pour la demande {}", request.getUuid(), exception);
            request.setStatus(AsyncRequestStatus.ERREUR);
            requestRepository.saveAndFlush(request);
        }
    }
}
