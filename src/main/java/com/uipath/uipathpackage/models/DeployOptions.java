package com.uipath.uipathpackage.models;

import java.util.List;

public class DeployOptions extends AuthenticatedOptions {
    private String packagesPath;
    private List<String> environments;

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
}
