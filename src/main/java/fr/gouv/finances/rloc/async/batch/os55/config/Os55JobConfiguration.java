package fr.gouv.finances.rloc.async.batch.os55.config;

import java.nio.file.Path;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.finances.rloc.async.batch.os55.Os55ProcessedItem;
import fr.gouv.finances.rloc.async.batch.os55.processor.Os55ItemProcessor;
import fr.gouv.finances.rloc.async.batch.os55.reader.Os55ItemReader;
import fr.gouv.finances.rloc.async.batch.os55.writer.Os55ItemWriter;
import fr.gouv.finances.rloc.async.common.error.ErreurDTOFactory;
import fr.gouv.finances.rloc.async.config.Os55BatchProperties;
import fr.gouv.finances.rloc.async.entity.AsyncRequestEntity;
import fr.gouv.finances.rloc.async.repository.AsyncRequestRepository;
import fr.gouv.finances.rloc.async.repository.Os55SurveillanceRepository;
import fr.gouv.finances.rloc.async.rest.os55.dto.Os55DemandeDTO;
import fr.gouv.finances.rloc.async.service.os55.Os55BusinessService;
import fr.gouv.finances.rloc.async.service.os55.Os55ResultService;

@Configuration
@ConditionalOnBean({ Os55BusinessService.class, Os55SurveillanceRepository.class })
public class Os55JobConfiguration {

    @Bean
    Job os55Job(JobRepository jobRepository, Step os55Step, Os55JobListener os55JobListener) {
        return new JobBuilder("os55Job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(os55JobListener)
                .start(os55Step)
                .build();
    }

    @Bean
    Step os55Step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader<Os55DemandeDTO> os55ItemReader,
            ItemProcessor<Os55DemandeDTO, Os55ProcessedItem> os55ItemProcessor,
            ItemWriter<Os55ProcessedItem> os55ItemWriter,
            Os55BatchProperties properties) {
        int chunkSize = properties.chunkSize() > 0 ? properties.chunkSize() : 100;
        return new StepBuilder("os55Step", jobRepository)
                .<Os55DemandeDTO, Os55ProcessedItem>chunk(chunkSize, transactionManager)
                .reader(os55ItemReader)
                .processor(os55ItemProcessor)
                .writer(os55ItemWriter)
                .build();
    }

    @Bean
    @StepScope
    ItemReader<Os55DemandeDTO> os55ItemReader(@Value("#{jobParameters['requestUuid']}") String requestUuid,
            AsyncRequestRepository requestRepository, ObjectMapper objectMapper) {
        AsyncRequestEntity request = requestRepository.findByUuid(UUID.fromString(requestUuid)).orElseThrow();
        return new Os55ItemReader(Path.of(request.getInputFilePath()), objectMapper);
    }

    @Bean
    ItemProcessor<Os55DemandeDTO, Os55ProcessedItem> os55ItemProcessor(ErreurDTOFactory erreurDTOFactory,
            Os55BusinessService businessService) {
        return new Os55ItemProcessor(erreurDTOFactory, businessService);
    }

    @Bean
    @StepScope
    ItemWriter<Os55ProcessedItem> os55ItemWriter(@Value("#{jobParameters['requestUuid']}") String requestUuid,
            AsyncRequestRepository asyncRequestRepository,
            Os55SurveillanceRepository surveillanceRepository,
            Os55ResultService resultService,
            ErreurDTOFactory erreurDTOFactory) {
        return new Os55ItemWriter(UUID.fromString(requestUuid), asyncRequestRepository, surveillanceRepository,
                resultService, erreurDTOFactory);
    }

    @Bean
    Os55JobListener os55JobListener(AsyncRequestRepository requestRepository, Os55ResultService resultService) {
        return new Os55JobListener(requestRepository, resultService);
    }
}
