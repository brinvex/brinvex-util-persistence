/*
 * Copyright © 2023 Brinvex (dev@brinvex.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brinvex.util.persistence.dba.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class PostgresqlDbaUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDbaUtilTest.class);

    @EnabledIfSystemProperty(named = "enableHostSystemAffectingTests", matches = "true")
    @Test
    public void install_uninstall() throws IOException {
        Path testBasePath = Paths.get("c:/prj/brinvex/brinvex-util/brinvex-util-persistence/BrinvexDbaTest");
        PostgresqlDbaConf conf = new PostgresqlDbaConf()
                .setName("BrinvexDbaTest")
                .setPgHomePath(testBasePath.resolve("postgresql"))
                .setSuperPass("S3cr3t!123")
                .setPort(5431)
                .setInstallerPath(testBasePath.resolve("install/postgresql-15.2-1-windows-x64.exe"))
                .addAllowedClientAddresses(List.of("192.168.0.0/16", "172.17.0.0/16"))
                .addExtensions(List.of("btree_gist"))
                .addAppUsers(Map.of("bx_app_user", "bx_app_user_123"))
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
                ));

        LOG.debug("install - {}", conf);
        try {
            PostgresqlDbaUtil.install(conf);
        } catch (Exception e) {
            LOG.info("Starting cleaning after install failed");
            try {
                PostgresqlDbaUtil.uninstall(conf);
                LOG.info("Cleaning successful");
            } catch (Exception e2) {
                LOG.warn("Cleaning failed - do it manually!!!", e);
            }
            throw e;
        }
        PostgresqlDbaUtil.uninstall(conf);
    }
}
