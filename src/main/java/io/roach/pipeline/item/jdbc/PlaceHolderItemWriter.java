package io.roach.pipeline.item.jdbc;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.roach.pipeline.cloud.ResourceResolver;

public class PlaceHolderItemWriter {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private DataSource dataSource;

        private String createQuery;

        private String insertQuery;

        public Builder setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder setCreateQuery(String createQuery) {
            this.createQuery = createQuery;
            return this;
        }

        public Builder setInsertQuery(String insertQuery) {
            this.insertQuery = insertQuery;
            return this;
        }

        public <T> ItemWriter<List<T>> build() {
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

            JdbcBatchItemWriter<List<T>> itemWriter = new JdbcBatchItemWriter<>();
            itemWriter.setDataSource(dataSource);
            itemWriter.setSql(insertQuery);
            itemWriter.setAssertUpdates(true);
            itemWriter.setItemPreparedStatementSetter((values, preparedStatement) -> {
                int i = 1;
                for (T v : values) {
                    preparedStatement.setObject(i++, v);
                }
            });
            itemWriter.afterPropertiesSet();
            return itemWriter;
        }
    }
}
