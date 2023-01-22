package io.roach.pipeline.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeline.template")
public class TemplateProperties {
    private boolean enabled = true;

    private int pollTimeoutSeconds;

    private int chunkSize;

    private int concurrency;

    private DataSourceProperties source;

    private DataSourceProperties target;

    private KafkaProperties kafka;

    public boolean isEnabled() {
        return enabled;
    }

    public TemplateProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public KafkaProperties getKafka() {
        return kafka;
    }

    public void setKafka(KafkaProperties kafka) {
        this.kafka = kafka;
    }

    public int getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(int pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public DataSourceProperties getSource() {
        return source;
    }

    public DataSourceProperties getTarget() {
        return target;
    }

    public void setSource(DataSourceProperties source) {
        this.source = source;
    }

    public void setTarget(DataSourceProperties target) {
        this.target = target;
    }
}
