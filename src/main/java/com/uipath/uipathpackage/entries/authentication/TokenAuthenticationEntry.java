package com.uipath.uipathpackage.entries.authentication;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
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
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * Represents the token authentication entry.
 */
public class TokenAuthenticationEntry extends SelectEntry {

    private final String credentialsId;
    private final String accountName;

    /**
     * Constructs a new instance of a token authentication entry
     * @param credentialsId The credentials id
     * @param accountName The account name
     */
    @DataBoundConstructor
    public TokenAuthenticationEntry(String credentialsId, String accountName) {
        this.credentialsId = credentialsId;
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    /**
     * Gets the token credentials
     * @return String credentialsId
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(credentialsId, "Invalid token credentials");
        return true;
    }

    @Symbol("Token")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.TokenAuthenticationEntry_DisplayName();
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
            return CredentialsProvider.listCredentials(StringCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.always());
        }

        /**
         * Validates Credentials if exists
         *
         * @param item  Basic configuration unit in Hudson
         * @param value Any conditional parameter(here id of the credential selected)
         * @return FormValidation
         */
        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            if (CredentialsProvider.listCredentials(StringCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.GenericErrors_MissingCredentialSecret());
            }

            return FormValidation.ok();
        }
    }
}