package io.roach.pipeline.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableConfigurationProperties(BatchProperties.class)
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1) // Bump up one level to enable extra advisors
public class BatchConfiguration extends DefaultBatchConfiguration {
    @Autowired
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    @Bean
    public BatchDataSourceScriptDatabaseInitializer batchDataSourceScriptDatabaseInitializer(DataSource dataSource,
                                                                                             BatchProperties properties) {
        return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
    }

    @Bean
    public DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer() throws MetaDataAccessException {
        return super.getIncrementerFactory().getIncrementer(getDatabaseType(), "batch");
    }

    @Override
    public JobLauncher jobLauncher() throws BatchConfigurationException {
        TaskExecutorJobLauncher taskExecutorJobLauncher = new TaskExecutorJobLauncher();
        taskExecutorJobLauncher.setJobRepository(jobRepository());
        taskExecutorJobLauncher.setTaskExecutor(taskExecutor);

        try {
            taskExecutorJobLauncher.afterPropertiesSet();
            return taskExecutorJobLauncher;
        } catch (Exception e) {
            throw new BatchConfigurationException("Unable to configure the default job launcher", e);
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(getDataSource());
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        transactionManager.setRollbackOnCommitFailure(true);
        return transactionManager;
    }
}
