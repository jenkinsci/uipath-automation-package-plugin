package com.uipath.uipathpackage.entries.testExecutionTarget;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

/**
 * Implementation of the test package entry
 */
public class TestProjectEntry extends SelectEntry {

    private final String testProjectPath;
    private final String environments;

    /**
     * Class to represent the test package entry
     * @param testProjectPath The path to the test project
     * @param environments Environments on which to test
     */
    @DataBoundConstructor
    public TestProjectEntry(String testProjectPath,
                            String environments) {
        this.testProjectPath = testProjectPath;
        this.environments = environments;
    }

    /**
     * Gets the test package path.
     * @return String testPackagePath
     */
    public String getTestProjectPath() {
        return testProjectPath;
    }

    /**
     * Gets the environments.
     * @return String environments
     */
    public String getEnvironments() {
        return environments;
    }

    @Override
    public boolean validateParameters() throws AbortException {
        Utility util = new Utility();
        util.validateParams(testProjectPath, "Invalid test package path");

        if (testProjectPath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException("Paths containing JENKINS_HOME are not allowed, use the Copy To Slave plugin to copy the required files to the slave's workspace instead.");
        }

        return true;
    }

    @Symbol("TestProject")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.TestProjectEntry_DescriptorImpl_DisplayName();
        }

        /**
         * Validates that the test project path is specified
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Test project path
         * @return FormValidation
         */
        public FormValidation doCheckTestProjectPath(@AncestorInPath Item item,
                                                     @QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.GenericErrors_MissingTestProjectPath());
            }

            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }
    }
}