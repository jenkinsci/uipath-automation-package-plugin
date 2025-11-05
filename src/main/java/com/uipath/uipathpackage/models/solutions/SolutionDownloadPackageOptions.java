package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options for solution package download operation.
 */
public class SolutionDownloadPackageOptions extends CommonOptions {
    private String packageName;
    private String packageVersion;
    private String destinationPath;
    private String fileName;

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

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}

