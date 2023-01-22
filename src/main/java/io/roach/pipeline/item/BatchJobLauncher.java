package io.roach.pipeline.item;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.support.DataFieldMaxValueJobParametersIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;

@Component
public class BatchJobLauncher {
    private static final Logger logger = LoggerFactory.getLogger(BatchJobLauncher.class);

    @SuppressWarnings("unchecked")
    public static <I, O> ItemProcessor<I, O> passThroughItemProcessor() {
        return item -> (O) item;
    }

    public static <T> ItemReadListener<List<T>> loggingReadListener(Resource inputResource) {
        return new LoggingResourceReadListener<>(inputResource, false);
    }

    public static <T> ItemReadListener<T> loggingReadListener() {
        return new LoggingReadListener<>();
    }

    public static <T> ItemWriteListener<T> loggingWriteListener() {
        return new LoggingWriteListener<>();
    }

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public <I, O> JobExecution submitJob(BatchJobManifest batchJobManifest,
                                         ItemReader<I> itemReader,
                                         ItemWriter<O> itemWriter) throws JobExecutionException {
        return submitJob(batchJobManifest, itemReader, itemWriter,
                passThroughItemProcessor(),
                loggingReadListener(),
                loggingWriteListener()
        );
    }

    public <I, O> JobExecution submitJob(BatchJobManifest batchJobManifest,
                                         ItemReader<? extends I> itemReader,
                                         ItemWriter<? super O> itemWriter,
                                         ItemProcessor<I, O> itemProcessor,
                                         ItemReadListener<? extends I> readListener,
                                         ItemWriteListener<? extends O> writeListener) throws JobExecutionException {
        SimpleStepBuilder<I, O> stepBuilder = new StepBuilder("processingStep", jobRepository)
                .<I, O>chunk(batchJobManifest.getChunkSize())
                .reader(itemReader)
                .processor(itemProcessor)
                .transactionManager(transactionManager)
                .writer(itemWriter)
                .listener(readListener)
                .listener(writeListener);

        if (batchJobManifest.isFaultTolerant()) {
            stepBuilder = stepBuilder.faultTolerant()
                    .retryPolicy(new MaxAttemptsRetryPolicy(10))
                    .retry(TransientDataAccessException.class)
                    .retry(TransactionSystemException.class)
                    .backOffPolicy(new ExponentialRandomBackOffPolicy());

        }

//        if (jobManifest.getConcurrency() > 1) {
//            stepBuilder.taskExecutor(taskExecutor)
//                    .throttleLimit(jobManifest.getConcurrency());
//        }

        Step step = stepBuilder.build();

        Job job = new JobBuilder(batchJobManifest.getName(), jobRepository)
                .incrementer(new DataFieldMaxValueJobParametersIncrementer(dataFieldMaxValueIncrementer))
                .flow(step)
                .end()
                .build();

        try {
            jobRegistry.register(new ReferenceJobFactory(job));
        } catch (DuplicateJobException e) {
            logger.info("Job already registered [{}]", e.getMessage()); // its ok
        }

        try {
            if (batchJobManifest.getRestartExecutionId() != 0) {
                long executionId = jobOperator.restart(batchJobManifest.getRestartExecutionId());
                logger.debug("Restart job with old execution ID {} - got new ID: {}",
                        batchJobManifest.getRestartExecutionId(),
                        executionId);
                return jobExplorer.getJobExecution(executionId);
            } else {
                JobParameters jobParameters = batchJobManifest.getJobParameters();
                return jobLauncher.run(job, jobParameters);
            }
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException
                 | NoSuchJobExecutionException | NoSuchJobException
                 | JobRestartException | JobParametersInvalidException e) {
            throw new JobExecutionException("Error starting job", e);
        }
    }
}
