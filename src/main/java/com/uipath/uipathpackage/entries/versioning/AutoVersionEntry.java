package com.uipath.uipathpackage.entries.versioning;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Implementation of the auto versioning method
 */
public class AutoVersionEntry extends SelectEntry {

    /**
     * Blank Class to represent auto versioning
     */
    @DataBoundConstructor
    public AutoVersionEntry() {
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Symbol("AutoVersion")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.AutoEntry_DescriptorImpl_DisplayName();
        }
    }
}