package com.uipath.uipathpackage.models.solutions;

import com.uipath.uipathpackage.models.CommonOptions;

/**
 * Options for solution package download operation.
 */
public class SolutionUploadPackageOptions extends CommonOptions {
    private String solutionPackagePath;

    public void setSolutionPackagePath(String solutionPackagePath) {
        this.solutionPackagePath = solutionPackagePath;
    }

    public String getSolutionPackagePath() {
        return solutionPackagePath;
    }
}
