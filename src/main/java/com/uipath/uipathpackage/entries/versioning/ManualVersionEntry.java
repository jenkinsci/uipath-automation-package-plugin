package com.uipath.uipathpackage.entries.versioning;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

/**
 * Implementation of the Custom Versioning method, providing the text as input
 */
public class ManualVersionEntry extends SelectEntry {

    private final String version;

    /**
     * Custom Version as text
     *
     * @param version Custom version value
     */
    @DataBoundConstructor
    public ManualVersionEntry(String version) {
        this.version = version;
    }

    /**
     * Getter for the text
     *
     * @return String custom version
     */
    public String getVersion() {
        return version;
    }

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(version, "Invalid custom version");
        return true;
    }

    /**
     * Metadata about a configurable instance.
     * <p>
     * {@link Descriptor} is an object that has metadata about a {@link Describable}
     * object, and also serves as a factory (in a way this relationship is similar
     * to {@link Object}/{@link Class} relationship.
     *
     * @see Describable
     */
    @Symbol("CustomVersion")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ManualEntry_DescriptorImpl_DisplayName();
        }

        /**
         * Validates Version Input
         *
         * @param value version
         * @return FormValidation
         */
        public FormValidation doCheckVersion(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(Messages.ManualEntry_DescriptorImpl_Errors_MissingVerson());
            }
            return FormValidation.ok();
        }
    }
}
