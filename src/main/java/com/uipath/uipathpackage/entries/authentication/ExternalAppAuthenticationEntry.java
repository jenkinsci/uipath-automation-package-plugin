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
 * Represents the external application authentication entry
 */
public class ExternalAppAuthenticationEntry extends SelectEntry {

    private final String identityUrl;
    private final String accountForApp;
    private final String applicationId;
    private final String applicationSecret;
    private final String applicationScope;

    /**
     * Constructs a new instance of an external application authentication entry
     * @param accountForApp The account name
     * @param applicationId The external application id
     * @param applicationSecret The external application secret
     * @param applicationScope The external application scope(s)
     */
    @DataBoundConstructor
    public ExternalAppAuthenticationEntry(String accountForApp, String applicationId, String applicationSecret, String applicationScope, String identityUrl) {
        this.accountForApp = accountForApp;
        this.applicationId = applicationId;
        this.applicationSecret = applicationSecret;
        this.applicationScope = applicationScope;
        this.identityUrl = identityUrl;
    }

    /**
     * Gets the external application account name
     * @return String accountForApp
     */
    public String getAccountForApp() {
        return accountForApp;
    }

    /**
     * Gets the external application id
     * @return String applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets the external application secret
     * @return String applicationSecret
     */
    public String getApplicationSecret() {
        return applicationSecret;
    }

    /**
     * Gets the external application scope(s)
     * @return String applicationScope
     */
    public String getApplicationScope() {
        return applicationScope;
    }
    
    /**
     * Gets the identityUrl
     * @return String identityUrl
     */
    public String getIdentityUrl() {
		return identityUrl;
	}

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(applicationId, Messages.ExternalAppAuthenticationEntry_DescriptorImpl_Errors_MissingApplicationId());
        util.validateParams(applicationSecret, Messages.ExternalAppAuthenticationEntry_DescriptorImpl_Errors_MissingApplicationSecret());
        util.validateParams(applicationScope, Messages.ExternalAppAuthenticationEntry_DescriptorImpl_Errors_MissingApplicationScope());
        return true;
    }

    @Symbol("ExternalApp")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ExternalAppAuthenticationEntry_DisplayName();
        }

        /**
         * Returns the list of StringCredentials to be filled in choice
         * If item is null or doesn't have configure permission it will return empty list
         *
         * @param item Basic configuration unit in Hudson
         * @return ListBoxModel list of StringCredentials
         */
        public ListBoxModel doFillApplicationSecretItems(@AncestorInPath Item item) {
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
        public FormValidation doCheckApplicationSecret(@AncestorInPath Item item, @QueryParameter String value) {
            if (CredentialsProvider.listCredentials(StringCredentials.class, item, ACL.SYSTEM, Collections.emptyList(), CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.GenericErrors_MissingCredentialSecret());
            }

            return FormValidation.ok();
        }
    }
}