package com.uipath.uipathpackage.entries.assetsAction;

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
public class DeleteAssetsEntry extends SelectEntry {

    /**
     * Class to represent the test package entry
     */
    @DataBoundConstructor
    public DeleteAssetsEntry() {
    }

    @Override
    public boolean validateParameters() throws AbortException {
        return true;
    }

    @Symbol("DeployAssets")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.AssetsDeleteEntry_DescriptorImpl_DisplayName();
        }
    }
}