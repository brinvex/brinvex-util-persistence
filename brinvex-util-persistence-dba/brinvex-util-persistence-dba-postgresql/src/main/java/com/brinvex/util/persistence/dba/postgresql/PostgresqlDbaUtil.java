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

import com.brinvex.util.persistence.dba.common.OsCmdResult;
import com.brinvex.util.persistence.dba.common.OsCmdUtil;
import com.brinvex.util.persistence.dba.common.WindowsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@SuppressWarnings("SpellCheckingInspection")
public class PostgresqlDbaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDbaUtil.class);

    public static void install(PostgresqlDbaConf conf) throws IOException {
        LOG.info("install {}", conf);

        initPostgresqlHomeFolder(conf);

        extractInstaller(conf);

        initPostgresqlDatabaseCluster(conf);

        registerPostgresqlWinService(conf);

        startPotgresqlWinService(conf);

        allowClientAddresses(conf);

        alterPostgresqlConnectionsSettings(conf);

        alterSystemSettings(conf);

        createPostgresqlExtensions(conf);

        createAppUsers(conf);

        createFirewallRuleForPostgresqlConnections(conf);

        restartPostgresqlWinService(conf);

        LOG.info("install - successfull {}", conf);

    }

    public static void uninstall(PostgresqlDbaConf conf) throws IOException {
        LOG.info("uninstall {}", conf);

        unregisterPostgresqlWindowsService(conf);

        backupPostgresqlData(conf);

        deletePostgresqlSystemFolder(conf);

        removeFirewallRuleForPostgresqlConnections(conf);

        LOG.info("uninstall - successfull {}", conf);

    }

    public static void backupPostgresqlData(PostgresqlDbaConf conf) throws IOException {
        Path pgDataBackupParentPath = conf.getPgDataBackupParentPath();
        File pgDataBackupParentFolder = pgDataBackupParentPath.toFile();
        if (pgDataBackupParentFolder.exists()) {
            LOG.info("PG data backup parent folder exists: {}", pgDataBackupParentPath);
        } else {
            LOG.info("Creating PG data backup parent folder: {}", pgDataBackupParentPath);
            boolean mkdirs = pgDataBackupParentFolder.mkdirs();
            if (!mkdirs) {
                throw new IOException("PG data backup parent foled creation failed: " + pgDataBackupParentPath);
            }
        }
        Path pgDataPath = conf.getPgDataPath();
        if (!pgDataPath.toFile().exists()) {
            LOG.info("No PG data folder to backup: {}", pgDataPath);
        } else {
            Path pgDataFileName = pgDataPath.getFileName();
            String backupFileName = pgDataFileName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path pgDataBackupPath = pgDataBackupParentPath.resolve(backupFileName);
            LOG.info("Moving PG data folder to backup: {} -> {}", pgDataPath, pgDataBackupPath);
            Files.move(pgDataPath, pgDataBackupPath);
            LOG.info("PG Data backup successfull {}", pgDataBackupPath);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deletePostgresqlSystemFolder(PostgresqlDbaConf conf) throws IOException {
        Path pgSysPath = conf.getPgSysPath();
        File pgSysPathFile = pgSysPath.toFile();
        if (!pgSysPathFile.exists()) {
            LOG.info("PG system folder does not exist: {}", pgSysPath);
        } else {
            LOG.info("Deleting PG system folder: {}", pgSysPath);
            try (var dirStream = Files.walk(pgSysPath)) {
                dirStream.map(Path::toFile)
                        .sorted(Comparator.reverseOrder())
                        .forEach(File::delete);
            }
        }
    }

    public static void restartPostgresqlWinService(PostgresqlDbaConf conf) throws IOException {
        WindowsUtil.restartWinService(conf.getWinServiceName());
    }

    public static void createAppUsers(PostgresqlDbaConf conf) throws IOException {
        Map<String, String> pgExtensions = conf.getAppUsers();
        if (pgExtensions.isEmpty()) {
            LOG.info("No PG app users to create");
        } else {
            for (var e : pgExtensions.entrySet()) {
                String appUserName = e.getKey();
                String appUserPwd = e.getValue();

                OsCmdResult pgUsersCheckResult = executePsqlSuperCommand(conf, "\\du");
                if (pgUsersCheckResult.getOut().contains(appUserName)) {
                    LOG.info("PG App user already exists: {}", appUserName);
                } else {
                    LOG.info("Creating PG App user: {}", appUserName);
                    String psqlCmd = String.format("CREATE USER %s WITH PASSWORD '%s'", appUserName, appUserPwd);
                    OsCmdResult r = executePsqlSuperCommand(conf, psqlCmd);

                    String expectedOut = "CREATE ROLE";
                    boolean outIsOk = expectedOut.equals(r.getOut());
                    boolean errIsOk = r.getErr().isBlank();
                    if (!outIsOk || !errIsOk) {
                        throw new IllegalStateException(format("PG command failed: %s, %s", psqlCmd, r));
                    }
                }
            }
        }
    }

    public static void createPostgresqlExtensions(PostgresqlDbaConf conf) throws IOException {
        Set<String> pgExtensions = conf.getPgExtensions();
        if (pgExtensions.isEmpty()) {
            LOG.info("No PG extensions to create");
        } else {
            for (String pgExtension : pgExtensions) {
                LOG.info("Creating PG extension (if not exists): {}", pgExtension);
                String psqlCmd = String.format("CREATE EXTENSION IF NOT EXISTS %s;", pgExtension);
                OsCmdResult r = executePsqlSuperCommand(conf, psqlCmd);

                String expectedOut = "CREATE EXTENSION";
                boolean outIsOk = expectedOut.equals(r.getOut());

                String expectedErr = format("NOTICE:  extension \"%s\" already exists, skipping", pgExtension);
                boolean errIsOk = r.getErr().isBlank() || expectedErr.equals(r.getErr());

                if (!outIsOk || !errIsOk) {
                    throw new IllegalStateException(format("PG command failed: %s, %s", psqlCmd, r));
                }
            }
        }
    }

    public static void alterSystemSettings(PostgresqlDbaConf conf) throws IOException {
        Set<String> systemSettings = conf.getSystemSettings();
        if (systemSettings.isEmpty()) {
            LOG.info("No system settings to alter");
        } else {
            for (String systemSetting : systemSettings) {
                LOG.info("Altering PG system setting: {}", systemSetting);
                String psqlCmd = String.format("ALTER SYSTEM SET %s", systemSetting);
                OsCmdResult r = executePsqlSuperCommand(conf, psqlCmd);
                if (!"ALTER SYSTEM".equals(r.getOut()) || !r.getErr().isBlank()) {
                    throw new IllegalStateException(format("PG command failed: %s, %s", psqlCmd, r));
                }
            }
        }
    }

    public static void alterPostgresqlConnectionsSettings(PostgresqlDbaConf conf) throws IOException {
        String pgListenAddresses = conf.getPgListenAddresses();
        int pgPort = conf.getPort();
        LOG.info("Altering PG connection settings: listen_addresses={}, port={}", pgListenAddresses, pgPort);

        String confFileName = "postgresql.conf";
        Path pgConfPath = conf.getPgDataPath().resolve(confFileName);
        String pgHbaConfContent = Files.readString(pgConfPath);
        {
            String portLine = format("port = %s", pgPort);
            if (pgHbaConfContent.contains(portLine)) {
                LOG.debug("PG {} already contains: {}", confFileName, portLine);
            } else {
                LOG.debug("Adding %s to the PG {}: {}", confFileName, portLine);
                pgHbaConfContent = pgHbaConfContent.replace("# - Connection Settings -",
                        "# - Connection Settings -\r\n" + portLine);
            }
        }
        {
            String listenAddressesLine = format("listen_addresses = '%s'", pgListenAddresses);
            if (pgHbaConfContent.contains(listenAddressesLine)) {
                LOG.debug("PG {} already contains: {}", confFileName, listenAddressesLine);
            } else {
                LOG.debug("Adding %s to the PG {}: {}", confFileName, listenAddressesLine);
                pgHbaConfContent = pgHbaConfContent.replace("# - Connection Settings -",
                        "# - Connection Settings -\r\n" + listenAddressesLine);
            }
        }
        Files.writeString(pgConfPath, pgHbaConfContent, StandardOpenOption.TRUNCATE_EXISTING);

        String winServiceName = conf.getWinServiceName();
        if (WindowsUtil.winServiceIsRunning(winServiceName)) {
            WindowsUtil.restartWinService(winServiceName);
        }
    }

    public static OsCmdResult executePsqlCommand(
            Path psqlPath,
            String host,
            int port,
            String db, String user,
            String pwd,
            String psqlCmd
    ) throws IOException {
        return OsCmdUtil.exec(
                String.format("%s -U %s -h %s -p %s -d %s -c \"%s\"", psqlPath, user, host, port, db, psqlCmd),
                Set.of("PGPASSWORD=" + pwd));
    }

    public static OsCmdResult executePsqlSuperCommand(
            PostgresqlDbaConf conf,
            String psqlCmd
    ) throws IOException {
        return executePsqlCommand(
                conf.getPsqlPath(),
                "localhost",
                conf.getPort(),
                "postgres",
                conf.getSuperUser(),
                conf.getSuperPass(),
                psqlCmd);
    }

    private static void startPotgresqlWinService(PostgresqlDbaConf conf) throws IOException {
        String winServiceName = conf.getWinServiceName();
        if (WindowsUtil.winServiceIsRunning(winServiceName)) {
            LOG.info("PG WinService already started: {}", winServiceName);
        } else {
            LOG.info("Starting PG WinService: {}", winServiceName);
            WindowsUtil.startWinService(winServiceName);
        }
    }

    public static void registerPostgresqlWinService(PostgresqlDbaConf conf) throws IOException {
        Path pgCtlExePath = conf.getPgSysPath().resolve("bin/pg_ctl.exe");
        String winServiceName = conf.getWinServiceName();
        if (WindowsUtil.winServiceExists(winServiceName)) {
            LOG.info("Windows service already exists: {}", winServiceName);
        } else {
            LOG.info("Registering PG WinService: {}", winServiceName);
            OsCmdUtil.exec(String.format("%s register -N %s -D \"%s\"", pgCtlExePath, winServiceName, conf.getPgDataPath()));
        }
    }

    public static void unregisterPostgresqlWindowsService(PostgresqlDbaConf conf) throws IOException {
        Path pgCtlExePath = conf.getPgSysPath().resolve("bin/pg_ctl.exe");
        String winServiceName = conf.getWinServiceName();
        if (!WindowsUtil.winServiceExists(winServiceName)) {
            LOG.info("WinService already unregistered: {}", winServiceName);
        } else {
            if (WindowsUtil.winServiceIsRunning(winServiceName)) {
                LOG.info("Stopping PG WinService: {}", winServiceName);
                WindowsUtil.stopWinService(winServiceName);
            }
            LOG.info("Unregistering PG WinService: {}", winServiceName);
            OsCmdUtil.exec(String.format("%s unregister -N %s -D \"%s\"", pgCtlExePath, winServiceName, conf.getPgDataPath()));
        }
    }

    public static void allowClientAddresses(PostgresqlDbaConf conf) throws IOException {
        Set<String> allowedClientAddresses = conf.getAllowedClientAddresses();
        if (allowedClientAddresses.isEmpty()) {
            LOG.info("No client addresses to allow");
        } else {
            LOG.info("Adding allowed clients to pg_hba.conf: {}", allowedClientAddresses);
            Path pgHbaConfPath = conf.getPgDataPath().resolve("pg_hba.conf");
            String pgHbaConf = Files.readString(pgHbaConfPath);
            for (String allowedClientAddress : allowedClientAddresses) {
                if (pgHbaConf.contains(allowedClientAddress)) {
                    LOG.debug("PG pg_hba.conf already contains: {}", allowedClientAddress);
                } else {
                    LOG.debug("Adding allowed client to PG pg_hba.conf: {}", allowedClientAddress);
                    pgHbaConf = pgHbaConf.replace("# IPv4 local connections:",
                            "# IPv4 local connections: \r\n" +
                            String.format("host    all    all    %s    scram-sha-256", allowedClientAddress));
                }
            }
            Files.writeString(pgHbaConfPath, pgHbaConf, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    public static void extractInstaller(PostgresqlDbaConf conf) throws IOException {
        Path pgSysPath = conf.getPgSysPath().toAbsolutePath();
        if (pgSysPath.toFile().exists()) {
            LOG.info("PG system folder already exists - skipping installer extractions: {}", pgSysPath);
        } else {
            Path installerPath = conf.getInstallerPath();
            if (installerPath == null || !installerPath.toFile().exists()) {
                throw new IllegalStateException(String.format("Installer not found: %s", installerPath));
            }
            String superUser = conf.getSuperUser();
            String superPass = conf.getSuperPass();
            if (superPass == null || superPass.isBlank()) {
                throw new IllegalStateException("Superpass can not be null");
            }
            LOG.info("Extracting PG installer: {}, pgHome={}", installerPath, pgSysPath);
            OsCmdUtil.exec(installerPath +
                           " --mode unattended " +
                           " --enable-components server,commandlinetools " +
                           " --disable-components pgAdmin,stackbuilder " +
                           " --create_shortcuts 0 " +
                           " --prefix " + pgSysPath + " " +
                           " --superaccount " + superUser + " " +
                           " --superpassword " + superPass + " " +
                           " --extract-only 1");
        }
    }

    public static void initPostgresqlHomeFolder(PostgresqlDbaConf conf) throws IOException {
        Path pgHomePath = conf.getPgHomePath();
        if (pgHomePath.toFile().exists()) {
            LOG.info("PG home folder already exists: {}", pgHomePath);
        } else {
            LOG.info("Creating PG home folder: {}", pgHomePath);
            boolean mkdirs = pgHomePath.toFile().mkdirs();
            if (!mkdirs) {
                throw new IOException(String.format("PG home folder creation failed: %s", pgHomePath));
            }
        }
    }

    public static void initPostgresqlDatabaseCluster(PostgresqlDbaConf conf) throws IOException {
        Path pgDataPath = conf.getPgDataPath().toAbsolutePath();
        if (pgDataPath.toFile().exists()) {
            LOG.info("PG data folder already exists - skipping PG database cluster initialization: {}", pgDataPath);
        } else {
            LOG.info("Initializing PG database cluster: pgData={}", pgDataPath);
            Path pgPwTmpFilePath = pgDataPath.resolve("../install_tmp");
            String superUser = conf.getSuperUser();
            String superPass = conf.getSuperPass();
            try {
                Files.writeString(pgPwTmpFilePath, superPass, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                Path pgInitExePath = conf.getPgSysPath().resolve("bin/initdb.exe");
                OsCmdUtil.exec(String.format("%s -D %s  -U %s --pwfile %s --auth=scram-sha-256 --locale=\"%s\"",
                        pgInitExePath, pgDataPath, superUser, pgPwTmpFilePath, conf.getPgLocale()));

            } finally {
                Files.deleteIfExists(pgPwTmpFilePath);
            }
        }
    }

    public static void createFirewallRuleForPostgresqlConnections(PostgresqlDbaConf conf) throws IOException {
        String firewallRuleName = conf.getFirewallRuleName();
        if (!WindowsUtil.firewallRuleExists(firewallRuleName)) {
            LOG.info("Creating firewall rule: {}", firewallRuleName);
            WindowsUtil.createTcpOpenFirewallRule(firewallRuleName, conf.getPort());
        } else {
            LOG.info("Firewall rule already exists: {}", firewallRuleName);
        }
    }

    public static void removeFirewallRuleForPostgresqlConnections(PostgresqlDbaConf conf) throws IOException {
        String firewallRuleName = conf.getFirewallRuleName();
        if (!WindowsUtil.firewallRuleExists(firewallRuleName)) {
            LOG.info("Firewall rule does not exist: {}", firewallRuleName);
        } else {
            LOG.info("Removing firewall rule: {}", firewallRuleName);
            WindowsUtil.removeFirewallRule(firewallRuleName, conf.getPort());
        }
    }
}
