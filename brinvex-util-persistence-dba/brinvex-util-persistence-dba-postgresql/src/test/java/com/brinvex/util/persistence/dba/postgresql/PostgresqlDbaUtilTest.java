package com.brinvex.util.persistence.dba.postgresql;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class PostgresqlDbaUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDbaUtilTest.class);

    @Test
    public void install_uninstall() throws IOException {
        Path basePath = Paths.get("c:/prj/brinvex/brinvex-util/brinvex-util-persistence/BrinvexDbaTest");
        PostgresqlDbaConf conf = new PostgresqlDbaConf()
                .setName("BrinvexDbaTest")
                .setPgHomePath(basePath.resolve("pg_server"))
                .setSuperPass("S3cr3t!123")
                .setPort(5431)
                .setInstallerPath(basePath.resolve("install/postgresql-15.2-1-windows-x64.exe"))
                .addAllowedClientAddresses(List.of("192.168.0.0/16", "172.17.0.0/16"))
                .addSystemSettings(List.of(
                        "max_connections = '100'",
                        "max_prepared_transactions = '100'",
                        "shared_buffers = '8GB'",
                        "effective_cache_size = '24GB'",
                        "maintenance_work_mem = '2097151kB'",
                        "checkpoint_completion_target = '0.9'",
                        "wal_buffers = '16MB'",
                        "default_statistics_target = '100'",
                        "random_page_cost = '1.1'",
                        "work_mem = '20971kB'",
                        "min_wal_size = '1GB'",
                        "max_wal_size = '4GB'",
                        "max_worker_processes = '8'",
                        "max_parallel_workers_per_gather = '4'",
                        "max_parallel_workers = '8'",
                        "max_parallel_maintenance_workers = '4'"
                ))
                .addExtensions(List.of("btree_gist"))
                .addAppUsers(Map.of("bx_app_user", "bx_app_user_123"));

        LOG.debug("install - {}", conf);

        try {
            PostgresqlDbaUtil.install(conf);
        } finally {
            LOG.info("Starting cleaning");
            try {
                PostgresqlDbaUtil.uninstall(conf);
                LOG.info("Cleaning successful");
            } catch (Exception e) {
                LOG.warn("Cleaning failed - do it manually!!!", e);
            }
        }
    }
}
