package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options for solution package deletion operation.
 */
public class SolutionDeletePackageOptions extends CommonOptions {
    private String packageName;
    private String packageVersion;

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
}

