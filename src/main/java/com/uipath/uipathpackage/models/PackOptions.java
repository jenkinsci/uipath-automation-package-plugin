package com.uipath.uipathpackage.models;

public class PackOptions extends AuthenticatedOptions {
    private String projectPath;
    private String destinationFolder;
    private String outputType;
    private String version;
    private boolean autoVersion;
    private final String telemetryOrigin = "Jenkins";

    public boolean getAutoVersion() {
        return autoVersion;
    }

    public void setAutoVersion(boolean autoVersion) {
        this.autoVersion = autoVersion;
    }

    public String getTelemetryOrigin() {
        return telemetryOrigin;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
