package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options model for activating a previously deployed solution deployment.
 */
public class SolutionDeployActivateOptions extends CommonOptions {
    private String deploymentName;

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }
}
