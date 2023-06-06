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
package com.brinvex.util.persistence.dba.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class DbaInstallConf {

    private final DbaConf baseConf;
    private String envName;
    private String firewallRuleName;
    private String winServiceName;
    private Path installerPath;
    private String dbLocale = "English_United States.UTF8";
    private String dbListenAddresses = "*";
    private final Set<String> allowedClientAddresses = new LinkedHashSet<>();
    private final Set<String> systemSettings = new LinkedHashSet<>();
    private final Set<String> dbExtensions = new LinkedHashSet<>();
    private final Map<String, String> appUsers = new LinkedHashMap<>();
    private final Map<String, String> appDatabases = new LinkedHashMap<>();

    public DbaInstallConf(DbaConf baseConf) {
        this.baseConf = baseConf;
    }

    public DbaConf getBaseConf() {
        return baseConf;
    }

    public String getEnvName() {
        return envName == null ? "BrinvexDBA" : envName;
    }

    public DbaInstallConf setEnvName(String envName) {
        this.envName = envName;
        return this;
    }

    public String getFirewallRuleName() {
        if (firewallRuleName != null) {
            return firewallRuleName;
        }
        return String.format("%s_PG - open %s", envName, baseConf.getPort());
    }

    public DbaInstallConf setFirewallRuleName(String firewallRuleName) {
        this.firewallRuleName = firewallRuleName;
        return this;
    }

    public String getWinServiceName() {
        return winServiceName == null ? (getEnvName() + "_Postgresql") : winServiceName;
    }

    public DbaInstallConf setWinServiceName(String winServiceName) {
        this.winServiceName = winServiceName;
        return this;
    }

    public Path getInstallerPath() {
        return installerPath;
    }

    public DbaInstallConf setInstallerPath(Path installerPath) {
        this.installerPath = installerPath;
        return this;
    }

    public String getDbLocale() {
        return dbLocale;
    }

    public DbaInstallConf setDbLocale(String dbLocale) {
        this.dbLocale = dbLocale;
        return this;
    }

    public Set<String> getAllowedClientAddresses() {
        return allowedClientAddresses;
    }

    public DbaInstallConf addAllowedClientAddresses(Collection<String> allowedClientAddresses) {
        this.allowedClientAddresses.addAll(allowedClientAddresses);
        return this;
    }

    public String getDbListenAddresses() {
        return dbListenAddresses;
    }

    public DbaInstallConf setDbListenAddresses(String dbListenAddresses) {
        this.dbListenAddresses = dbListenAddresses;
        return this;
    }

    public Set<String> getSystemSettings() {
        return systemSettings;
    }

    public DbaInstallConf addSystemSettings(Collection<String> systemSettings) {
        this.systemSettings.addAll(systemSettings);
        return this;
    }

    public Set<String> getDbExtensions() {
        return dbExtensions;
    }

    public DbaInstallConf addExtensions(Collection<String> extensions) {
        this.dbExtensions.addAll(extensions);
        return this;
    }

    public Map<String, String> getAppUsers() {
        return appUsers;
    }

    public DbaInstallConf addAppUsers(Map<String, String> appUsers) {
        this.appUsers.putAll(appUsers);
        return this;
    }

    public Map<String, String> getAppDatabases() {
        return appDatabases;
    }

    public DbaInstallConf addAppDatabases(Map<String, String> appDatabases) {
        this.appDatabases.putAll(appDatabases);
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DbaInstallConf.class.getSimpleName() + "[", "]")
                .add("baseConf=" + baseConf)
                .add("envName='" + envName + "'")
                .add("firewallRuleName='" + firewallRuleName + "'")
                .add("winServiceName='" + winServiceName + "'")
                .add("installerPath=" + installerPath)
                .add("pgLocale='" + dbLocale + "'")
                .add("pgListenAddresses='" + dbListenAddresses + "'")
                .add("allowedClientAddresses=" + allowedClientAddresses)
                .add("systemSettings=" + systemSettings)
                .add("pgExtensions=" + dbExtensions)
                .add("appUsers=" + appUsers)
                .add("appDatabases=" + appDatabases)
                .toString();
    }
}
