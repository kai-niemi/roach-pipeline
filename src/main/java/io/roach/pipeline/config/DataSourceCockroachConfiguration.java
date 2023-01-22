package io.roach.pipeline.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@ApplicationProfiles.PostgreSQL
@ApplicationProfiles.CockroachDB
public class DataSourceCockroachConfiguration {
    @Bean
    public DataSourceProperties internalDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return hikariDataSource();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource() {
        return internalDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
