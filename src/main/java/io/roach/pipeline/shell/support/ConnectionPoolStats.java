package io.roach.pipeline.shell.support;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

public class ConnectionPoolStats {
    public static ConnectionPoolStats from(HikariDataSource ds) {
        ConnectionPoolStats ps = new ConnectionPoolStats();
        HikariPoolMXBean mxBean = ds.getHikariPoolMXBean();
        if (mxBean != null) {
            ps.activeConnections = mxBean.getActiveConnections();
            ps.idleConnections = mxBean.getIdleConnections();
            ps.threadsAwaitingConnection = mxBean.getThreadsAwaitingConnection();
            ps.totalConnections = mxBean.getTotalConnections();
            return ps;
        }
        return ps;
    }

    public int activeConnections;

    public int idleConnections;

    public int threadsAwaitingConnection;

    public int totalConnections;
}
