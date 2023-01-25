package io.roach.pipeline.web;

/**
 * Domain specific link relations.
 */
public abstract class LinkRels {
    public static final String DS_TABLES_REL = "datasource-tables";
    
    public static final String JOBS_REL = "jobs";

    public static final String JOB_INSTANCE_REL = "instance";

    public static final String JOB_LAST_INSTANCE_REL = "last-instance";

    public static final String JOB_INSTANCES_REL = "instances";

    public static final String JOB_EXECUTION_REL = "execution";

    public static final String JOB_EXECUTIONS_REL = "executions";

    public static final String JOB_LAST_EXECUTION_REL = "last-execution";

    public static final String STEP_EXECUTION_REL = "step";

    public static final String STEP_EXECUTIONS_REL = "steps";

    public static final String STOP_REL = "stop";

    public static final String FLAT2CSV_REL = "flat2csv";

    public static final String FLAT2SQL_REL = "flat2sql";

    public static final String SQL2CSV_REL = "sql2csv";

    public static final String SQL2SQL_REL = "sql2sql";

    public static final String CDC2SQL_REL = "cdc2sql";

    public static final String KAFKA2SQL_REL = "kafka2sql";

    public static final String CDC2SQL_SINK_REL = "cdc2sql-sink";

    public static final String ACTUATOR_REL = "actuator";

    public static final String HAL_EXPLORER_REL = "hal-explorer";

    public static final String TEMPLATE_REL = "template";

    public static final String BUNDLE_SUFFIX = "-bundle";

    public static final String ZIP_BUNDLE_SUFFIX = "-zip-bundle";

    // IANA standard link relations:
    // http://www.iana.org/assignments/link-relations/link-relations.xhtml

    public static final String CURIE_NAMESPACE = "pipeline";

    public static final String CURIE_PREFIX = CURIE_NAMESPACE + ":";

    private LinkRels() {
    }
}
