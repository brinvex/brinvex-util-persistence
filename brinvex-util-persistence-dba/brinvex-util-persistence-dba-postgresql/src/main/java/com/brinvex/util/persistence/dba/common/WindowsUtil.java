package com.brinvex.util.persistence.dba.common;

import java.io.IOException;

public class WindowsUtil {

    public static void startWinService(String winServiceName) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format("net start %s", winServiceName));
        if (!r.getOut().endsWith(String.format("The %s service was started successfully.", winServiceName))) {
            throw new IOException(String.format("Service starting failed: %s %s", winServiceName, r));
        }
    }

    public static void startWinServiceIfNotRunning(String winServiceName) throws IOException {
        if (!winServiceIsRunning(winServiceName)) {
            startWinService(winServiceName);
        }
    }

    public static void stopWinService(String winServiceName) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format("net stop %s", winServiceName));
        if (!r.getOut().endsWith(String.format("The %s service was stopped successfully.", winServiceName))) {
            throw new IOException(String.format("Service stopping failed: %s %s", winServiceName, r));
        }
    }

    public static void restartWinService(String winServiceName) throws IOException {
        if (winServiceIsRunning(winServiceName)) {
            stopWinService(winServiceName);
        }
        startWinService(winServiceName);
    }

    public static boolean winServiceIsRunning(String winServiceName) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format("sc query %s", winServiceName));
        if (!r.getOut().contains(winServiceName)) {
            throw new IllegalArgumentException(String.format("Service state not available: %s %s", winServiceName, r));
        }
        return r.getOut().contains("RUNNING");
    }

    public static boolean winServiceIsStopped(String winServiceName) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format("sc query %s", winServiceName));
        if (!r.getOut().contains(winServiceName)) {
            throw new IllegalArgumentException(String.format("Service state not available: %s %s", winServiceName, r));
        }
        return r.getOut().contains("STOPPED");
    }

    public static boolean winServiceExists(String winServiceName) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format("sc query %s", winServiceName));
        return r.getOut().contains(winServiceName);
    }

    public static boolean firewallRuleExists(String ruleName) throws IOException {
        OsCmdResult r1 = OsCmdUtil.exec(String.format("netsh advfirewall firewall show rule name=\"%s\"", ruleName));
        return r1.getOut().contains(ruleName);
    }

    public static void createTcpOpenFirewallRule(String ruleName, int localPort) throws IOException {
        OsCmdResult r = OsCmdUtil.exec(String.format(
                "netsh advfirewall firewall add rule name=\"%s\" dir=in action=allow protocol=TCP localport=%s",
                ruleName, localPort));
        if (!"Ok.".equals(r.getOut())) {
            throw new IOException(String.format("Firewall rule creation failed: %s", r));
        }
    }


}
