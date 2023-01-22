package io.roach.pipeline.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
@ApplicationProfiles.H2
public class DataSourceH2Configuration {
    @Bean
    @Primary
    public DataSource dataSource() {
        EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
        return embeddedDatabaseBuilder
                .addScript("classpath:org/springframework/batch/core/schema-drop-h2.sql")
                .addScript("classpath:org/springframework/batch/core/schema-h2.sql")
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}
