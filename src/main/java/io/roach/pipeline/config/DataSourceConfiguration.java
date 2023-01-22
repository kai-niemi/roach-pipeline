package io.roach.pipeline.config;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import com.zaxxer.hikari.HikariDataSource;

import io.roach.pipeline.util.DataSourceProps;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
public class DataSourceConfiguration {
    public static final String SQL_TRACE_LOGGER = "io.roach.pipeline.SQL_TRACE";

    @Bean
    @Lazy
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Function<DataSourceProps, ClosableDataSource> dataSourceFactory() {
        return props -> {
            DataSourceProperties properties = new DataSourceProperties();
            properties.setUrl(props.getUrl());
            properties.setUsername(props.getUserName());
            properties.setPassword(props.getPassword());
            properties.setName(props.getName());

            HikariDataSource ds = properties
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
            ds.setMaximumPoolSize(Math.max(1, props.getConcurrency()));
            ds.setMinimumIdle(Math.max(1, props.getConcurrency()));
            ds.setKeepaliveTime(Duration.ofSeconds(60).toMillis()); // 60,000
            ds.setMaxLifetime(Duration.ofMinutes(15).toMillis()); // 900,000
            ds.setConnectionTimeout(Duration.ofSeconds(18).toMillis()); // 18,000
            ds.setPoolName(props.getName());
            ds.setAutoCommit(true);
            ds.addDataSourceProperty("reWriteBatchedInserts", "true");
            ds.addDataSourceProperty("application_name", props.getName());

            return new ClosableDataSource(ProxyDataSourceBuilder
                    .create(ds)
                    .name(props.getName())
                    .asJson()
                    .logQueryBySlf4j(SLF4JLogLevel.TRACE, SQL_TRACE_LOGGER)
                    .multiline()
                    .build());
        };
    }
}
