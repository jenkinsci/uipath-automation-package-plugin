package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options for deploying a solution/package into an Orchestrator context.
 */
public class SolutionDeployOptions extends CommonOptions {
    private String packageName;
    private String packageVersion;
    private String deploymentName;
    private String deploymentFolderName;
    private String deploymentParentFolder;
    private String configPath;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentFolderName(String deploymentFolderName) {
        this.deploymentFolderName = deploymentFolderName;
    }

    public String getDeploymentFolderName() {
        return deploymentFolderName;
    }

    public void setDeploymentParentFolder(String deploymentParentFolder) {
        this.deploymentParentFolder = deploymentParentFolder;
    }

    public String getDeploymentParentFolder() {
        return deploymentParentFolder;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getConfigPath() {
        return configPath;
    }
}

