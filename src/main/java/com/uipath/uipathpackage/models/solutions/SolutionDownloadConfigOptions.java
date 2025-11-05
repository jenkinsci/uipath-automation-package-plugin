package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;
import com.uipath.uipathpackage.util.ConfigFormat;

/**
 * Options for solution configuration download operation.
 */
public class SolutionDownloadConfigOptions extends CommonOptions {
    private String packageName;
    private String packageVersion;
    private String destinationPath;
    private String fileName;
    private ConfigFormat format;

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

    public void setFormat(ConfigFormat format) {
        this.format = format;
    }

    public ConfigFormat getFormat() {
        return format;
    }
}
