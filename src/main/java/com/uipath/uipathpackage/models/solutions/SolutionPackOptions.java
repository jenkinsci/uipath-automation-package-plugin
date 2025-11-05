package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

public class SolutionPackOptions extends CommonOptions {
    private String projectPath;
    private String destinationFolder;
    private Boolean disableBuiltInNugetFeeds;
    private String repositoryUrl;
    private String repositoryCommit;
    private String repositoryBranch;
    private String repositoryType;
    private String projectUrl;
    private String releaseNotes;
    private String version;

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

    public boolean isDisableBuiltInNugetFeeds() {
        return disableBuiltInNugetFeeds != null && disableBuiltInNugetFeeds;
    }

    public void setDisableBuiltInNugetFeeds(Boolean disableBuiltInNugetFeeds) {
        this.disableBuiltInNugetFeeds = disableBuiltInNugetFeeds;
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

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }
}
