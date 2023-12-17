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
public class PostgresqlDbaManager implements DbaManager {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresqlDbaManager.class);

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void install(DbaInstallConf conf) throws IOException {
        LOG.info("install {}", conf);

        var baseConf = conf.getBaseConf();
        var winServiceName = conf.getWinServiceName();

        initDbHomeFolder(baseConf.getDbHomePath());

        installDbSystem(baseConf, conf.getInstallerPath());

        initMainDatabase(baseConf, conf.getDbLocale());

        registerDbWinService(baseConf, winServiceName);

        startDbWinService(winServiceName);

        allowClientAddresses(baseConf, conf.getAllowedClientAddresses());

        alterConnectionsSettings(baseConf, conf.getDbListenAddresses());

        restartDbSystem(conf);

        alterSystemSettings(baseConf, conf.getSystemSettings());

        createAppDbUsers(baseConf, conf.getAppUsers());

        createAppDatabases(baseConf, conf.getAppDatabases());

        installDbExtensions(baseConf, conf.getAppDatabases(), conf.getAppUsers(), conf.getDbExtensions());

        createFirewallRule(baseConf, conf.getFirewallRuleName());

        restartDbSystem(conf);

        LOG.info("install successfull {}", conf);
    }

    @Override
    public void uninstall(DbaInstallConf conf) throws IOException {
        LOG.info("uninstall {}", conf);

        var baseConf = conf.getBaseConf();

        unregisterDbWinService(baseConf, conf.getWinServiceName());

        backupAllDbData(baseConf);

        uninstallDbSystem(baseConf);

        removeFirewallRule(conf.getFirewallRuleName());

        LOG.info("uninstall successfull {}", conf);

    }

    @Override
    public void backupDatabase(DbaConf conf, String dbToBackup, Path backupPath) throws IOException {
        var pgDumpPath = conf.getDbToolsPath().resolve("pg_dump");
        var host = conf.getHost();
        var port = conf.getPort();
        var superUser = conf.getSuperUser();
        var superPass = conf.getSuperPass();
        backupDatabase(pgDumpPath, dbToBackup, backupPath, host, port, superUser, superPass);
    }

    @Override
    public void restoreDatabase(
            DbaConf conf,
            Path backupPath,
            String db,
            String owner
    ) throws IOException {
        var dbExists = databaseExists(conf, db);
        LOG.info("restore {} from backup {}, dbExists={}, {}", db, backupPath, dbExists, conf);
        if (dbExists) {
            throw new IllegalArgumentException(String.format("Database already exists: %s", db));
        }

        createDatabase(conf, db, owner);

        var pgRestorePath = conf.getDbToolsPath().resolve("pg_restore");
        var host = conf.getHost();
        var port = conf.getPort();
        var user = conf.getSuperUser();
        var pass = conf.getSuperPass();
        restoreDatabase(pgRestorePath, backupPath, host, port, user, pass, db, owner);
    }

    @Override
    public void backupAndDropDatabase(DbaConf conf, String db) throws IOException {
        var dbDataBackupParentFolder = prepareDbDataBackupParentFolder(conf);
        var ts = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        var oldDbDataBackupFolderName = format("%s_%s.backup", db, ts);
        var oldDbDataBackupFolderPath = dbDataBackupParentFolder.resolve(oldDbDataBackupFolderName);
        LOG.info("restore {} - creating backup of old DB {}", db, oldDbDataBackupFolderPath);
        backupDatabase(conf, db, oldDbDataBackupFolderPath);
        LOG.info("restore {} - dropping old DB after successfull backup {}", db, oldDbDataBackupFolderPath);
        riskyDropDatabase(conf, db);
    }

    @Override
    public void riskyDropDatabase(DbaConf conf, String db) throws IOException {
        LOG.info("Drop DB {}, {}", db, conf);
        executePsqlSuperCommand(conf, String.format("DROP DATABASE %s WITH (FORCE); ", db));
    }

    @Override
    public void restartDbSystem(DbaInstallConf conf) throws IOException {
        String winServiceName = conf.getWinServiceName();
        WindowsUtil.restartWinService(winServiceName);
    }

    @Override
    public void restartDbSystemIfRunning(DbaInstallConf conf) throws IOException {
        String winServiceName = conf.getWinServiceName();
        if (WindowsUtil.winServiceIsRunning(winServiceName)) {
            WindowsUtil.restartWinService(winServiceName);
        }
    }

    @Override
    public void backupAllDbData(DbaConf conf) throws IOException {
        var dbDataPath = conf.getDbDataPath();
        var pgDataBackupParentPath = prepareDbDataBackupParentFolder(conf);
        if (!dbDataPath.toFile().exists()) {
            LOG.info("No PG data folder to backup: {}", dbDataPath);
        } else {
            var pgDataFolderName = dbDataPath.getFileName().toString();
            var backupFolderName = pgDataFolderName + "_" + LocalDateTime.now().format(TIMESTAMP_FORMAT);
            var BackupFolderPath = pgDataBackupParentPath.resolve(backupFolderName);
            LOG.info("Moving PG data folder to backup: {} -> {}", dbDataPath, BackupFolderPath);
            Files.move(dbDataPath, BackupFolderPath);
            LOG.info("PG Data backup successfull {}", BackupFolderPath);
        }
    }

    @Override
    public void createAppDbUsers(DbaConf conf, Map<String, String> appUsers) throws IOException {
        if (appUsers.isEmpty()) {
            LOG.info("No PG app users to create");
        } else {
            for (var e : appUsers.entrySet()) {
                String appUserName = e.getKey();
                String appUserPwd = e.getValue();

                OsCmdResult pgUsersCheckResult = executePsqlSuperCommand(conf, "\\du");
                if (pgUsersCheckResult.getOut().contains(appUserName)) {
                    LOG.info("PG App user already exists: {}", appUserName);
                } else {
                    LOG.info("Creating PG App user: {}", appUserName);
                    String psqlCmd = format("CREATE USER %s WITH PASSWORD '%s'", appUserName, appUserPwd);
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

    @Override
    public void createAppDatabases(DbaConf conf, Map<String, String> appDbs) throws IOException {
        if (appDbs.isEmpty()) {
            LOG.info("No PG app databases to create");
        } else {
            for (var e : appDbs.entrySet()) {
                String appDbName = e.getKey();
                String appDbOwner = e.getValue();

                boolean dbExists = databaseExists(conf, appDbName);
                if (dbExists) {
                    LOG.info("PG App DB already exists: {}", appDbName);
                } else {
                    LOG.info("Creating PG App DB {} with owner {}", appDbName, appDbOwner);
                    createDatabase(conf, appDbName, appDbOwner);
                }
            }
        }
    }

    @Override
    public boolean databaseExists(DbaConf conf, String db) throws IOException {
        var psqlPath = conf.getDbToolsPath().resolve("psql");
        var host = conf.getHost();
        var port = conf.getPort();
        var user = conf.getSuperUser();
        var pass = conf.getSuperPass();
        return databaseExists(psqlPath, host, port, db, user, pass);
    }

    private boolean databaseExists(
            Path psqlPath, String host, int port, String db, String user, String pwd
    ) throws IOException {
        var cmd = format("%s -U %s -h %s -p %s -XtA -c \"SELECT 1 FROM pg_database WHERE datname='%s'\"", psqlPath, user, host, port, db);
        var envs = Set.of("PGPASSWORD=" + pwd);
        OsCmdResult r = OsCmdUtil.exec(cmd, envs);
        return "1".equals(r.getOut());
    }

    private void restoreDatabase(
            Path pgRestorePath,
            Path dbBackupPath,
            String host,
            int port,
            String user,
            String pwd,
            String newDbName,
            String newOwner
    ) throws IOException {
        dbBackupPath = dbBackupPath.toAbsolutePath();
        LOG.info("Restore DB {} from backup {}, host={}, port={}, newOwner={}", newDbName, dbBackupPath, host, port, newOwner);
        var cmd = format("%s -U %s -h %s -p %s -d %s --no-owner --role=%s %s",
                pgRestorePath, user, host, port, newDbName, newOwner, dbBackupPath);
        var envs = Set.of("PGPASSWORD=" + pwd);
        OsCmdResult r = OsCmdUtil.exec(cmd, envs);
        if (!r.getOut().isBlank() || !r.getErr().isBlank()) {
            throw new IOException(format("DB restore failed: %s", r));
        }
    }

    private void backupDatabase(
            Path pgDumpPath, String dbName, Path backupPath, String host, int port, String user, String pwd
    ) throws IOException {
        backupPath = backupPath.toAbsolutePath();
        LOG.info("Backup DB {}, host={}, port={}, backupPath={}", dbName, host, port, backupPath);
        String cmd = format("%s -Fc -d postgresql://%s:%s@%s/%s --port %s --encoding UTF-8 --file %s",
                pgDumpPath, user, pwd, host, dbName, port, backupPath);
        OsCmdResult r = OsCmdUtil.exec(cmd);
        if (!r.getOut().isBlank() || !r.getErr().isBlank()) {
            throw new IOException(format("DB backup failed: %s", r));
        }
    }

    private void createDatabase(DbaConf conf, String db, String owner) throws IOException {
        LOG.info("createDatabase - {}, owner={}, {}", db, owner, conf);
        OsCmdResult r = executePsqlSuperCommand(conf, format("CREATE DATABASE %s WITH OWNER='%s'", db, owner));
        if (!"CREATE DATABASE".equals(r.getOut()) || !r.getErr().isBlank()) {
            throw new IOException(format("Database creation failed: %s, %s, %s", db, owner, r));
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void installDbExtensions(
            DbaConf conf,
            Map<String, String> appDatabases,
            Map<String, String> appUsers,
            Set<String> extensions
    ) throws IOException {
        if (extensions.isEmpty()) {
            LOG.info("No PG extensions to create");
        } else {
            for (String appDb : appDatabases.keySet()) {
                String appUser = appDb;
                String appPwd = appUsers.get(appDb);
                for (String extension : extensions) {
                    LOG.info("Creating PG extension in DB {} (if not exists): {}", appDb, extension);
                    String psqlCmd = format("CREATE EXTENSION IF NOT EXISTS %s;", extension);
                    OsCmdResult r = executePsqlAppUserCommand(conf, psqlCmd, appUser, appPwd, appDb);

                    String expectedOut = "CREATE EXTENSION";
                    boolean outIsOk = expectedOut.equals(r.getOut());

                    String expectedErr = format("NOTICE:  extension \"%s\" already exists, skipping", extension);
                    boolean errIsOk = r.getErr().isBlank() || expectedErr.equals(r.getErr());

                    if (!outIsOk || !errIsOk) {
                        throw new IllegalStateException(format("PG command failed: %s, %s", psqlCmd, r));
                    }
                }
            }
        }
    }

    private void alterSystemSettings(DbaConf conf, Set<String> systemSettings) throws IOException {
        if (systemSettings.isEmpty()) {
            LOG.info("No system settings to alter");
        } else {
            for (String systemSetting : systemSettings) {
                LOG.info("Altering PG system setting: {}", systemSetting);
                String psqlCmd = format("ALTER SYSTEM SET %s", systemSetting);
                OsCmdResult r = executePsqlSuperCommand(conf, psqlCmd);
                if (!"ALTER SYSTEM".equals(r.getOut()) || !r.getErr().isBlank()) {
                    throw new IllegalStateException(format("PG command failed: %s, %s", psqlCmd, r));
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void alterConnectionsSettings(DbaConf conf, String listenAddresses) throws IOException {
        var pgPort = conf.getPort();
        LOG.info("Altering PG connection settings: listen_addresses={}, port={}", listenAddresses, pgPort);

        var confFileName = "postgresql.conf";
        var pgConfPath = conf.getDbDataPath().resolve(confFileName);
        var pgHbaConfContent = Files.readString(pgConfPath);
        {
            String portLine = format("port = %s", pgPort);
            if (pgHbaConfContent.contains(portLine)) {
                LOG.debug("PG {} already contains: {}", confFileName, portLine);
            } else {
                LOG.debug("Adding %s to the PG {}: {}", confFileName, portLine);
                String oldLine = "# - Connection Settings -";
                String newLines = "# - Connection Settings -\r\n" + portLine;
                pgHbaConfContent = pgHbaConfContent.replace(oldLine, newLines);
            }
        }
        {
            String listenAddressesLine = format("listen_addresses = '%s'", listenAddresses);
            if (pgHbaConfContent.contains(listenAddressesLine)) {
                LOG.debug("PG {} already contains: {}", confFileName, listenAddressesLine);
            } else {
                LOG.debug("Adding %s to the PG {}: {}", confFileName, listenAddressesLine);
                String oldLine = "# - Connection Settings -";
                String newLines = "# - Connection Settings -\r\n" + listenAddressesLine;
                pgHbaConfContent = pgHbaConfContent.replace(oldLine, newLines);
            }
        }
        Files.writeString(pgConfPath, pgHbaConfContent, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SuppressWarnings("SameParameterValue")
    private OsCmdResult executePsqlCommand(
            Path psqlPath, String host, int port, String db, String user, String pwd, String psqlCmd
    ) throws IOException {
        var cmd = format("%s -U %s -h %s -p %s -d %s -c \"%s\"", psqlPath, user, host, port, db, psqlCmd);
        var envs = Set.of("PGPASSWORD=" + pwd);
        return OsCmdUtil.exec(cmd, envs);
    }

    private OsCmdResult executePsqlSuperCommand(
            DbaConf conf,
            String psqlCmd
    ) throws IOException {
        return executePsqlSuperCommand(conf, psqlCmd, "postgres");
    }

    private OsCmdResult executePsqlSuperCommand(
            DbaConf conf,
            String psqlCmd,
            String db
    ) throws IOException {
        var psqlPath = conf.getDbToolsPath().resolve("psql");
        var host = conf.getHost();
        var port = conf.getPort();
        var user = conf.getSuperUser();
        var pass = conf.getSuperPass();
        return executePsqlCommand(psqlPath, host, port, db, user, pass, psqlCmd);
    }

    private OsCmdResult executePsqlAppUserCommand(
            DbaConf conf,
            String psqlCmd,
            String user,
            String pass,
            String db
    ) throws IOException {
        var psqlPath = conf.getDbToolsPath().resolve("psql");
        var host = conf.getHost();
        var port = conf.getPort();
        return executePsqlCommand(psqlPath, host, port, db, user, pass, psqlCmd);
    }

    private void startDbWinService(String winServiceName) throws IOException {
        if (WindowsUtil.winServiceIsRunning(winServiceName)) {
            LOG.info("PG WinService already started: {}", winServiceName);
        } else {
            LOG.info("Starting PG WinService: {}", winServiceName);
            WindowsUtil.startWinService(winServiceName);
        }
    }

    private void registerDbWinService(DbaConf baseConf, String winServiceName) throws IOException {
        Path pgCtlExePath = baseConf.getDbToolsPath().resolve("pg_ctl.exe");
        if (WindowsUtil.winServiceExists(winServiceName)) {
            LOG.info("Windows service already exists: {}", winServiceName);
        } else {
            LOG.info("Registering PG WinService: {}", winServiceName);
            OsCmdUtil.exec(format("%s register -N %s -D \"%s\"", pgCtlExePath, winServiceName, baseConf.getDbDataPath()));
        }
    }

    private void unregisterDbWinService(DbaConf conf, String winServiceName) throws IOException {
        Path pgCtlExePath = conf.getDbToolsPath().resolve("pg_ctl.exe");
        if (!WindowsUtil.winServiceExists(winServiceName)) {
            LOG.info("WinService already unregistered: {}", winServiceName);
        } else {
            if (WindowsUtil.winServiceIsRunning(winServiceName)) {
                LOG.info("Stopping PG WinService: {}", winServiceName);
                WindowsUtil.stopWinService(winServiceName);
            }
            LOG.info("Unregistering PG WinService: {}", winServiceName);
            OsCmdUtil.exec(format("%s unregister -N %s -D \"%s\"", pgCtlExePath, winServiceName, conf.getDbDataPath()));
        }
    }

    private void allowClientAddresses(DbaConf conf, Set<String> allowedClientAddresses1) throws IOException {
        if (allowedClientAddresses1.isEmpty()) {
            LOG.info("No client addresses to allow");
        } else {
            LOG.info("Adding allowed clients to pg_hba.conf: {}", allowedClientAddresses1);
            Path pgHbaConfPath = conf.getDbDataPath().resolve("pg_hba.conf");
            String pgHbaConf = Files.readString(pgHbaConfPath);
            for (String allowedClientAddress : allowedClientAddresses1) {
                if (pgHbaConf.contains(allowedClientAddress)) {
                    LOG.debug("PG pg_hba.conf already contains: {}", allowedClientAddress);
                } else {
                    LOG.debug("Adding allowed client to PG pg_hba.conf: {}", allowedClientAddress);
                    String oldLine = "# IPv4 local connections:";
                    String newLines = "# IPv4 local connections: \r\n" +
                                      format("host    all    all    %s    scram-sha-256", allowedClientAddress);
                    pgHbaConf = pgHbaConf.replace(oldLine, newLines);
                }
            }
            Files.writeString(pgHbaConfPath, pgHbaConf, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void installDbSystem(DbaConf conf, Path installerPath) throws IOException {
        Path pgSysPath = conf.getDbSystemPath().toAbsolutePath();
        if (pgSysPath.toFile().exists()) {
            LOG.info("PG system folder already exists - skipping installer extractions: {}", pgSysPath);
        } else {
            if (installerPath == null || !installerPath.toFile().exists()) {
                throw new IllegalStateException(format("Installer not found: %s", installerPath));
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

    private void initDbHomeFolder(Path dbHomePath) throws IOException {
        if (dbHomePath.toFile().exists()) {
            LOG.info("PG home folder already exists: {}", dbHomePath);
        } else {
            LOG.info("Creating PG home folder: {}", dbHomePath);
            boolean mkdirs = dbHomePath.toFile().mkdirs();
            if (!mkdirs) {
                throw new IOException(format("PG home folder creation failed: %s", dbHomePath));
            }
        }
    }

    private void initMainDatabase(DbaConf conf, String dbLocale) throws IOException {
        Path pgDataPath = conf.getDbDataPath().toAbsolutePath();
        if (pgDataPath.toFile().exists()) {
            LOG.info("PG data folder already exists - skipping PG database cluster initialization: {}", pgDataPath);
        } else {
            LOG.info("Initializing PG database cluster: pgData={}", pgDataPath);
            Path pgPwTmpFilePath = pgDataPath.resolve("../install_tmp");
            String superUser = conf.getSuperUser();
            String superPass = conf.getSuperPass();
            try {
                Files.writeString(pgPwTmpFilePath, superPass, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                Path pgInitExePath = conf.getDbToolsPath().resolve("initdb.exe");
                OsCmdUtil.exec(format("%s -D %s  -U %s --pwfile %s --auth=scram-sha-256 --locale=\"%s\"",
                        pgInitExePath, pgDataPath, superUser, pgPwTmpFilePath, dbLocale));

            } finally {
                Files.deleteIfExists(pgPwTmpFilePath);
            }
        }
    }

    private void createFirewallRule(DbaConf conf, String firewallRuleName) throws IOException {
        if (!WindowsUtil.firewallRuleExists(firewallRuleName)) {
            LOG.info("Creating firewall rule: {}", firewallRuleName);
            WindowsUtil.createTcpOpenFirewallRule(firewallRuleName, conf.getPort());
        } else {
            LOG.info("Firewall rule already exists: {}", firewallRuleName);
        }
    }

    private void removeFirewallRule(String firewallRuleName) throws IOException {
        if (!WindowsUtil.firewallRuleExists(firewallRuleName)) {
            LOG.info("Firewall rule does not exist: {}", firewallRuleName);
        } else {
            LOG.info("Removing firewall rule: {}", firewallRuleName);
            WindowsUtil.removeFirewallRule(firewallRuleName);
        }
    }

    private Path prepareDbDataBackupParentFolder(DbaConf conf) throws IOException {
        var dbDataPath = conf.getDbDataPath();
        var dbDataFolderName = dbDataPath.getFileName();
        var dbDataBackupParentFolderName = dbDataFolderName + "_backup";
        var dbDataBackupParentFolderPath = dbDataPath.getParent().resolve(dbDataBackupParentFolderName);

        File pgDataBackupParentFolder = dbDataBackupParentFolderPath.toFile();
        if (pgDataBackupParentFolder.exists()) {
            LOG.info("PG data backup parent folder exists: {}", dbDataBackupParentFolderPath);
        } else {
            LOG.info("Creating PG data backup parent folder: {}", dbDataBackupParentFolderPath);
            boolean mkdirs = pgDataBackupParentFolder.mkdirs();
            if (!mkdirs) {
                throw new IOException("PG data backup parent foled creation failed: " + dbDataBackupParentFolderPath);
            }
        }
        return dbDataBackupParentFolderPath;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void uninstallDbSystem(DbaConf conf) throws IOException {
        var pgSysPath = conf.getDbSystemPath();
        if (!pgSysPath.toFile().exists()) {
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


}
