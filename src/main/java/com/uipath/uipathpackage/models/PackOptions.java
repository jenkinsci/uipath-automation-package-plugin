package com.uipath.uipathpackage.models;

public class PackOptions extends CommonOptions {
    private String projectPath;
    private String destinationFolder;
    private String outputType;
    private Boolean splitOutput;
    private Boolean disableBuiltInNugetFeeds;
    private String repositoryUrl;
    private String repositoryCommit;
    private String repositoryBranch;
    private String repositoryType;
    private String projectUrl;
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

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public boolean isSplitOutput() {
        return splitOutput;
    }

    public boolean isDisableBuiltInNugetFeeds() {
        return disableBuiltInNugetFeeds;
    }

    public void setDisableBuiltInNugetFeeds(Boolean disableBuiltInNugetFeeds) {
        this.disableBuiltInNugetFeeds = disableBuiltInNugetFeeds;
    }

    public void setSplitOutput(Boolean splitOutput) {
        this.splitOutput = splitOutput;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getRepositoryCommit() {
        return repositoryCommit;
    }

    public void setRepositoryCommit(String repositoryCommit) {
        this.repositoryCommit = repositoryCommit;
    }

    public String getRepositoryBranch() {
        return repositoryBranch;
    }

    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }
}
