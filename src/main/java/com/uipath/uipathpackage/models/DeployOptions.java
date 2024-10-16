package com.uipath.uipathpackage.models;

import java.util.List;

public class DeployOptions extends CommonOptions {
    private String packagesPath;
    private List<String> environments;
    private List<String> entryPointPaths;
    private boolean createProcess;
    private Boolean ignoreLibraryDeployConflict = null;
    private String processName;
    private String processNames;

    public String getPackagesPath() {
        return packagesPath;
    }

    public void setPackagesPath(String packagesPath) {
        this.packagesPath = packagesPath;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<String> environments) {
        this.environments = environments;
    }

    public List<String> getEntryPointPaths() {
        return entryPointPaths;
    }

    public void setEntryPointPaths(List<String> entryPointPaths) {
        this.entryPointPaths = entryPointPaths;
    }

    public boolean getCreateProcess() {
        return createProcess;
    }

    public void setCreateProcess(boolean createProcess) {
        this.createProcess = createProcess;
    }

    public boolean getIgnoreLibraryDeployConflict() {
        return ignoreLibraryDeployConflict;
    }

    public void setIgnoreLibraryDeployConflict(boolean ignoreLibraryDeployConflict) {
        this.ignoreLibraryDeployConflict = ignoreLibraryDeployConflict;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessNames() {
        return processNames;
    }

    public void setProcessNames(String processNames) {
        this.processNames = processNames;
    }
}
