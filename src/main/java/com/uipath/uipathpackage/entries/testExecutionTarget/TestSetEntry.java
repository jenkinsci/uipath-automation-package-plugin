package com.uipath.uipathpackage.entries.testExecutionTarget;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
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
 * Implementation of the test set entry
 */
public class TestSetEntry extends SelectEntry {

    private final String testSet;

    /**
     * Blank Class to represent the test set entry
     * @param testSet The test set to execute
     */
    @DataBoundConstructor
    public TestSetEntry(String testSet) {
        this.testSet = testSet;
    }

    /**
     * Gets the test set to be executed.
     * @return String testSet
     */
    public String getTestSet() {
        return testSet;
    }

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(testSet, "Invalid test set name");
        return true;
    }

    @Symbol("TestSet")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.TestSetEntry_DescriptorImpl_DisplayName();
        }

        /**
         * Validates that the test set is specified
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Test set
         * @return FormValidation
         */
        public FormValidation doCheckTestSet(@AncestorInPath Item item, @QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.GenericErrors_MissingTestSetName());
            }

            return FormValidation.ok();
        }
    }
}