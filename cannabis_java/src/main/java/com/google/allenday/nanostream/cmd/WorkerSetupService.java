package com.google.allenday.nanostream.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.stream.Stream;

public class WorkerSetupService implements Serializable {
    private Logger LOG = LoggerFactory.getLogger(WorkerSetupService.class);

    private final static String CMD_APT_UPDATE = "apt-get update";
    private final static String CMD_INSTALL_WGET = "apt-get install wget -y";
    private final static String CMD_INSTALL_BZIP2 = "apt-get install bzip2 -y";
    private final static String CMD_DOWNLOAD_MONIMAP = "wget https://github.com/lh3/minimap2/releases/download/v2.17/minimap2-2.17_x64-linux.tar.bz2";
    private final static String CMD_UNTAR_MINIMAP = "tar -xvjf minimap2-2.17_x64-linux.tar.bz2 -C /";

    private final static String[] MINIMAP2_INSTALL_COMMANDS =
            {
                    CMD_APT_UPDATE,
                    CMD_INSTALL_WGET,
                    CMD_INSTALL_BZIP2,
                    CMD_DOWNLOAD_MONIMAP,
                    CMD_UNTAR_MINIMAP
            };

    private CmdExecutor cmdExecutor;

    public WorkerSetupService(CmdExecutor cmdExecutor) {
        this.cmdExecutor = cmdExecutor;
    }

    private void setupByCommands(String[] commands) {
        LOG.info("Start setup");
        Stream.of(commands)
                .forEach(command -> cmdExecutor.executeCommand(command));
        LOG.info("Finish setup");
    }

    public void setupMinimap2() {
        setupByCommands(MINIMAP2_INSTALL_COMMANDS);
    }
}
