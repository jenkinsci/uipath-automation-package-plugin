package com.uipath.uipathpackage.entries.job;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class NonProductionJobTypeEntry extends SelectEntry {
    /**
     * Blank Class to represent non production job type
     */
    @DataBoundConstructor
    public NonProductionJobTypeEntry() {
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Symbol("NonProduction")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.NonProductionJobTypeEntry_DisplayName();
        }
    }
}
