package com.brinvex.util.persistence.dba.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

public class OsCmdUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OsCmdUtil.class);

    public static OsCmdResult exec(String cmds) throws IOException {
        return exec(cmds, Collections.emptyList());
    }

    public static OsCmdResult exec(String cmds, Collection<String> envs) throws IOException {
        String normCmds = cmds.replaceAll("\\s+", " ");
        LOG.debug("exec {} [envs={}]", normCmds, envs);

        String[] cmdParts = normCmds.split("\\s");
        Runtime runtime = Runtime.getRuntime();
        Process process = envs.isEmpty() ? runtime.exec(cmdParts) : runtime.exec(cmdParts, envs.toArray(String[]::new));

        StringBuilder outSb = new StringBuilder();
        BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        outReader.lines().forEach(s -> {
            LOG.trace("cmdOut: {}", s);
            if (!s.isBlank()) {
                outSb.append(s);
            }
        });
        outReader.close();

        StringBuilder errSb = new StringBuilder();
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        errReader.lines().forEach(s -> {
            LOG.trace("cmdErr: {}", s);
            if (!s.isBlank()) {
                errSb.append(s);
            }
        });
        errReader.close();

        return new OsCmdResult(outSb.toString(), errSb.toString());
    }

}
