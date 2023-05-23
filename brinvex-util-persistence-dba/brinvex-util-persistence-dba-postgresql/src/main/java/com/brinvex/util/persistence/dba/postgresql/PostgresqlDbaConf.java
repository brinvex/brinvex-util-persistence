package com.brinvex.util.persistence.dba.postgresql;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class PostgresqlDbaConf {

    private String name;
    private String firewallRuleName;
    private int port = 5432;
    private Path pgHomePath;
    private Path pgDataPath;
    private Path pgDataBackupParentPath;
    private String superUser;
    private String superPass;
    private String winServiceName;
    private Path installerPath;
    private Path psqlPath;
    private String pgLocale = "English_United States.UTF8";
    private String pgListenAddresses = "*";
    private final Set<String> allowedClientAddresses = new LinkedHashSet<>();
    private final Set<String> systemSettings = new LinkedHashSet<>();
    private final Set<String> pgExtensions = new LinkedHashSet<>();
    private final Map<String, String> appUsers = new LinkedHashMap<>();

    public String getName() {
        return name == null ? "BrinvexDBA" : name;
    }

    public PostgresqlDbaConf setName(String name) {
        this.name = name;
        return this;
    }

    public String getFirewallRuleName() {
        if (firewallRuleName != null) {
            return firewallRuleName;
        }
        return String.format("%s_PG - open %s", name, port);
    }

    public PostgresqlDbaConf setFirewallRuleName(String firewallRuleName) {
        this.firewallRuleName = firewallRuleName;
        return this;
    }

    public int getPort() {
        return port;
    }

    public PostgresqlDbaConf setPort(int port) {
        this.port = port;
        return this;
    }

    public Path getPgHomePath() {
        return pgHomePath == null ? pgHomePath = Paths.get("c:/", name, "pg_server") : pgHomePath;
    }

    public PostgresqlDbaConf setPgHomePath(Path pgHomePath) {
        this.pgHomePath = pgHomePath;
        return this;
    }

    public Path getPgDataPath() {
        return pgDataPath == null ? getPgHomePath().resolve("..").resolve("pg_data").normalize() : pgDataPath;
    }

    public PostgresqlDbaConf setPgDataPath(Path pgDataPath) {
        this.pgDataPath = pgDataPath;
        return this;
    }

    public String getSuperUser() {
        return superUser == null ? "postgresql" : superUser;
    }

    public PostgresqlDbaConf setSuperUser(String superUser) {
        this.superUser = superUser;
        return this;
    }

    public String getSuperPass() {
        return superPass;
    }

    public PostgresqlDbaConf setSuperPass(String superPass) {
        this.superPass = superPass;
        return this;
    }

    public String getWinServiceName() {
        return winServiceName == null ? (getName() + "_PG") : winServiceName;
    }

    public PostgresqlDbaConf setWinServiceName(String winServiceName) {
        this.winServiceName = winServiceName;
        return this;
    }

    public Path getInstallerPath() {
        return installerPath;
    }

    public PostgresqlDbaConf setInstallerPath(Path installerPath) {
        this.installerPath = installerPath;
        return this;
    }

    public Path getPsqlPath() {
        return psqlPath == null ? pgHomePath.resolve("bin/psql") : psqlPath;
    }

    public PostgresqlDbaConf setPsqlPath(Path psqlPath) {
        this.psqlPath = psqlPath;
        return this;
    }

    public String getPgLocale() {
        return pgLocale;
    }

    public PostgresqlDbaConf setPgLocale(String pgLocale) {
        this.pgLocale = pgLocale;
        return this;
    }

    public Set<String> getAllowedClientAddresses() {
        return allowedClientAddresses;
    }

    public PostgresqlDbaConf addAllowedClientAddresses(Collection<String> allowedClientAddresses) {
        this.allowedClientAddresses.addAll(allowedClientAddresses);
        return this;
    }

    public String getPgListenAddresses() {
        return pgListenAddresses;
    }

    public PostgresqlDbaConf setPgListenAddresses(String pgListenAddresses) {
        this.pgListenAddresses = pgListenAddresses;
        return this;
    }

    public Set<String> getSystemSettings() {
        return systemSettings;
    }

    public PostgresqlDbaConf addSystemSettings(Collection<String> systemSettings) {
        this.systemSettings.addAll(systemSettings);
        return this;
    }

    public Set<String> getPgExtensions() {
        return pgExtensions;
    }

    public PostgresqlDbaConf addExtensions(Collection<String> extensions) {
        this.pgExtensions.addAll(extensions);
        return this;
    }

    public Map<String, String> getAppUsers() {
        return appUsers;
    }

    public PostgresqlDbaConf addAppUsers(Map<String, String> appUsers) {
        this.appUsers.putAll(appUsers);
        return this;
    }

    public Path getPgDataBackupParentPath() {
        return pgDataBackupParentPath == null ? getPgDataPath().resolve("../pd_data_backup").normalize() : pgDataBackupParentPath;
    }

    public PostgresqlDbaConf setPgDataBackupParentPath(Path pgDataBackupParentPath) {
        this.pgDataBackupParentPath = pgDataBackupParentPath;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PostgresqlDbaConf.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("firewallRuleName='" + firewallRuleName + "'")
                .add("port=" + port)
                .add("pgHomePath=" + pgHomePath)
                .add("pgDataPath=" + pgDataPath)
                .add("pgDataBackupPath=" + pgDataBackupParentPath)
                .add("superUser='" + superUser + "'")
                .add("superPass='" + superPass + "'")
                .add("winServiceName='" + winServiceName + "'")
                .add("installerPath=" + installerPath)
                .add("psqlPath=" + psqlPath)
                .add("pgLocale='" + pgLocale + "'")
                .add("pgListenAddresses='" + pgListenAddresses + "'")
                .add("allowedClientAddresses=" + allowedClientAddresses)
                .add("systemSettings=" + systemSettings)
                .add("pgExtensions=" + pgExtensions)
                .add("appUsers=" + appUsers)
                .toString();
    }
}
