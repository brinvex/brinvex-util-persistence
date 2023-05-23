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

import com.brinvex.util.persistence.dba.api.DbaConf;
import com.brinvex.util.persistence.dba.api.DbaInstallConf;
import com.brinvex.util.persistence.dba.api.DbaManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresqlDbaTest {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDbaTest.class);

    @EnabledIfSystemProperty(named = "enableHostSystemAffectingTests", matches = "true")
    @Test
    public void all() throws IOException {
        Path testBasePath = Paths.get("c:/prj/brinvex/brinvex-util/brinvex-util-persistence/BrinvexDbaTest");

        String appUser = "bx_app1_user1";
        String appDb = "bx_app1";

        DbaConf baseConf = new DbaConf()
                .setPort(5431)
                .setSuperPass("S3cr3t!123")
                .setDbHomePath(testBasePath.resolve("postgresql"));

        DbaInstallConf installConf = new DbaInstallConf(baseConf)
                .setEnvName("BrinvexDbaTest")
                .setInstallerPath(testBasePath.resolve("install/postgresql-15.2-1-windows-x64.exe"))
                .addAllowedClientAddresses(List.of("192.168.0.0/16", "172.17.0.0/16"))
                .addExtensions(List.of("btree_gist"))
                .addAppUsers(Map.of(appUser, "bx_app_user1_123"))
                .addAppDatabases(Map.of(appDb, appUser))
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

        DbaManager dbaManager = new PostgresqlDbaManager();

        LOG.debug("uninstall - cleaning mess from previous runs - {}", installConf);
        dbaManager.uninstall(installConf);

        LOG.debug("install - {}", installConf);
        dbaManager.install(installConf);

        Path backupToRestore = testBasePath.resolve("db-bck/bx_app1-test-in.backup");
        try {
            dbaManager.restoreDatabase(baseConf, backupToRestore, appDb, appUser);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Database already exists"));
        }
        dbaManager.backupAndDropDatabase(baseConf, appDb);
        dbaManager.restoreDatabase(baseConf, backupToRestore, appDb, appUser);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupPath = testBasePath.resolve(String.format("db-bck/%s-test-out-%s.backup", appDb, ts));
        dbaManager.backupDatabase(baseConf, appDb, backupPath);

        LOG.debug("uninstall - {}", installConf);
        dbaManager.uninstall(installConf);
    }

}
