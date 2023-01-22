package io.roach.pipeline.item;

import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.util.Assert;

public class BatchJobManifest {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final BatchJobManifest instance = new BatchJobManifest();

        private final JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

        private Builder() {
        }

        public Builder withRestartExecutionId(long restartExecutionId) {
            instance.restartExecutionId = restartExecutionId;
            return this;
        }

        public Builder withRandomId() {
            instance.id = UUID.randomUUID();
            return this;
        }

        public Builder withId(UUID id) {
            instance.id = id;
            return this;
        }

        public Builder withName(String name) {
            instance.name = name;
            return this;
        }

        public Builder withChunkSize(int chunkSize) {
            instance.chunkSize = chunkSize;
            return this;
        }

        public Builder withConcurrency(int concurrency) {
            instance.concurrency = concurrency;
            return this;
        }

        public Builder withJobParameters(Consumer<JobParametersBuilder> builder) {
            builder.accept(jobParametersBuilder);
            return this;
        }

        public Builder withFaultTolerance() {
            instance.faultTolerant = true;
            return this;
        }

        public BatchJobManifest build() {
            Assert.notNull(instance.id, "id is required");
            Assert.hasLength(instance.name, "name is required");
            Assert.state(instance.chunkSize > 0, "chunkSize must be > 0");

            instance.jobParameters = jobParametersBuilder
                    .addString("jobId", instance.id.toString(), true)
                    .toJobParameters();

            return instance;
        }
    }

    private UUID id;

    private String name;

    private int chunkSize;

    private int concurrency = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

    private boolean faultTolerant;

    private long restartExecutionId;

    private JobParameters jobParameters;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    public JobParameters getJobParameters() {
        return jobParameters;
    }

    public long getRestartExecutionId() {
        return restartExecutionId;
    }

    public int getConcurrency() {
        return concurrency;
    }
}
