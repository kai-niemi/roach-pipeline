package io.roach.pipeline.item.jdbc;

import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.roach.pipeline.cloud.ResourceResolver;

public class NamedParameterItemWriter {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DataSource dataSource;

        private String createQuery;

        private String updateQuery;

        private boolean assertUpdates = true;

        public NamedParameterItemWriter.Builder setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public NamedParameterItemWriter.Builder setCreateQuery(String createQuery) {
            this.createQuery = createQuery;
            return this;
        }

        public NamedParameterItemWriter.Builder setUpdateQuery(String updateQuery) {
            this.updateQuery = updateQuery;
            return this;
        }

        public NamedParameterItemWriter.Builder setAssertUpdates(boolean assertUpdates) {
            this.assertUpdates = assertUpdates;
            return this;
        }

        public ItemWriter<Map<String, Object>> build() {
            Assert.notNull(dataSource, "dataSource is null");

            if (ResourceResolver.supportedPrefix(createQuery)) {
                ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
                databasePopulator.setCommentPrefix("--");
                databasePopulator.setIgnoreFailedDrops(true);

                Arrays.stream(createQuery.split(","))
                        .forEach(s -> databasePopulator.addScript(ResourceResolver.getResource(s)));

                DatabasePopulatorUtils.execute(databasePopulator, dataSource);
            } else if (StringUtils.hasLength(createQuery)) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.execute(createQuery);
            }

            JdbcBatchItemWriter<Map<String, Object>> itemWriter = new JdbcBatchItemWriter<>();
            itemWriter.setDataSource(dataSource);
            itemWriter.setSql(updateQuery);
            itemWriter.setAssertUpdates(assertUpdates);
            itemWriter.setItemSqlParameterSourceProvider(MapSqlParameterSource::new);
            itemWriter.afterPropertiesSet();

            return itemWriter;
        }
    }

}
