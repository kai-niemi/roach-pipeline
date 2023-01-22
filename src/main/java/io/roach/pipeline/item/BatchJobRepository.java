package io.roach.pipeline.item;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.roach.pipeline.shell.Console;
import jakarta.annotation.PostConstruct;

@Repository
public class BatchJobRepository {
    private static final String DELETE_EXECUTION_CONTEXT_SQL =
            "delete from %PREFIX%step_execution_context where 1 = 1";

    private static final String DELETE_STEP_EXECUTION_CONTEXT_SQL =
            "delete from %PREFIX%step_execution where 1 = 1";

    private static final String DELETE_JOB_EXECUTION_CONTEXT_SQL =
            "delete from %PREFIX%job_execution_context where 1 = 1";

    private static final String DELETE_JOB_EXECUTION_PARAMS_SQL =
            "delete from %PREFIX%job_execution_params where 1 = 1";

    private static final String DELETE_JOB_EXECUTION_SQL =
            "delete from %PREFIX%job_execution where 1 = 1";

    private static final String DELETE_JOB_INSTANCE_SQL =
            "delete from %PREFIX%job_instance where 1 = 1";

    private static final String DEFAULT_TABLE_PREFIX = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Console console;

    private JdbcTemplate jdbcTemplate;

    private String tablePrefix = DEFAULT_TABLE_PREFIX;

    @PostConstruct
    public void init() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    protected String getQuery(String base) {
        return StringUtils.replace(base, "%PREFIX%", tablePrefix);
    }

    public BatchJobRepository setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        return this;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeAll() {
        int rowsAffected = jdbcTemplate.update(getQuery(DELETE_EXECUTION_CONTEXT_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_EXECUTION_CONTEXT_SQL));

        rowsAffected += jdbcTemplate.update(getQuery(DELETE_STEP_EXECUTION_CONTEXT_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_STEP_EXECUTION_CONTEXT_SQL));

        rowsAffected += jdbcTemplate.update(getQuery(DELETE_JOB_EXECUTION_CONTEXT_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_JOB_EXECUTION_CONTEXT_SQL));

        rowsAffected += jdbcTemplate.update(getQuery(DELETE_JOB_EXECUTION_PARAMS_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_JOB_EXECUTION_PARAMS_SQL));

        rowsAffected += jdbcTemplate.update(getQuery(DELETE_JOB_EXECUTION_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_JOB_EXECUTION_SQL));

        rowsAffected += jdbcTemplate.update(getQuery(DELETE_JOB_INSTANCE_SQL));
        console.printf(AnsiColor.BRIGHT_GREEN, "Deleted %d rows using [%s]\n", rowsAffected,
                getQuery(DELETE_JOB_INSTANCE_SQL));
        return rowsAffected;
    }
}
