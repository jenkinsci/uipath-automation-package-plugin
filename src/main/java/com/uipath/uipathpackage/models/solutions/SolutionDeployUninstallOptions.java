package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options model for uninstalling (removing) a solution deployment.
 */
public class SolutionDeployUninstallOptions extends CommonOptions {
    private String deploymentName;

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }
}
