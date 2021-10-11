package com.uipath.uipathpackage.entries.job;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;

import hudson.Extension;
import hudson.model.Descriptor;

public class TestAutomationJobTypeEntry extends SelectEntry {
	
    /**
     * Blank Class to represent test automation job type
     */
    @DataBoundConstructor
    public TestAutomationJobTypeEntry() {
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Symbol("Test")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.TestAutomationJobTypeEntry_DisplayName();
        }
    }
}