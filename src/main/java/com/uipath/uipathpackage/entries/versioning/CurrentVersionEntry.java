package com.uipath.uipathpackage.entries.versioning;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Implementation of the current versioning method
 */
public class CurrentVersionEntry extends SelectEntry {

    /**
     * Blank Class to represent current versioning
     */
    @DataBoundConstructor
    public CurrentVersionEntry() {
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Symbol("CurrentVersion")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.CurrentEntry_DescriptorImpl_DisplayName();
        }
    }
}