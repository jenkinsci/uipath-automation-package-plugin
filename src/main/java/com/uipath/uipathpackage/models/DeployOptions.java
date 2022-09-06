package com.uipath.uipathpackage.models;

import java.util.List;

public class DeployOptions extends AuthenticatedOptions {
    private String packagesPath;
    private List<String> environments;
    private List<String> entryPointPaths;
    private boolean createProcess;
    private final String telemetryOrigin = "Jenkins";

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
    
    public String getTelemetryOrigin() {
        return telemetryOrigin;
    }

    public boolean getCreateProcess() {
        return createProcess;
    }

    public void setCreateProcess(boolean createProcess) {
        this.createProcess = createProcess;
    }
}
