package com.uipath.uipathpackage.models;

public class PackOptions implements SerializableCliOptions {
    private String projectPath;
    private String destinationFolder;
    private String version;
    private boolean autoVersion;

    public boolean getAutoVersion() {
        return autoVersion;
    }

    public void setAutoVersion(boolean autoVersion) {
        this.autoVersion = autoVersion;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
