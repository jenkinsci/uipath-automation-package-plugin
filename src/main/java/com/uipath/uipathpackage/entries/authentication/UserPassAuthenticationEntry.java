package com.uipath.uipathpackage.entries.authentication;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * Represents the username-password authentication entry
 */
public class UserPassAuthenticationEntry extends SelectEntry {

    private final String credentialsId;
    private static int numberOfItems = 0;

    /**
     * Constructs a new instance of a username-password authentication entry
     * @param credentialsId The credentials id
     */
    @DataBoundConstructor
    public UserPassAuthenticationEntry(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * Gets the username-password credentials
     * @return String credentialsId
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(credentialsId, "Invalid username-password credentials");
        return true;
    }

    @Symbol("UserPass")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.UserPassAuthenticationEntry_DisplayName();
        }

        /**
         * Returns the list of StandardUsernamePasswordCredentials to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of StandardUsernamePasswordCredentials
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            ListBoxModel result = CredentialsProvider.listCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.always());
            numberOfItems = result.size();
            return result;
        }

        /**
         * Validates Credentials if exists
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Any conditional parameter(here id of the credential selected)
         * @return FormValidation
         */
        public FormValidation doCheckCredentialsId(@AncestorInPath Item item,
                                                   @QueryParameter String value) {
            if (numberOfItems != 0 && item != null && value != null && value.trim().isEmpty()) {
                return FormValidation.ok();
            }

            if (value == null || CredentialsProvider.listCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.GenericErrors_MissingCredentialSet());
            }

            return FormValidation.ok();
        }
    }
}