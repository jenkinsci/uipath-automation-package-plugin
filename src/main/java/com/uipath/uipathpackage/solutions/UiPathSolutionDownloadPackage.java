package com.uipath.uipathpackage.solutions;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.models.solutions.SolutionDownloadPackageOptions;
import com.uipath.uipathpackage.util.CliDetails;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static hudson.slaves.WorkspaceList.tempDir;

public class UiPathSolutionDownloadPackage extends Recorder implements SimpleBuildStep {
    private final String packageName;
    private final String destinationPath;
    private String packageVersion;
    private String fileName;

    private final SelectEntry credentials;
    private final TraceLevel traceLevel;
    private final String orchestratorAddress;
    private final String orchestratorTenant;

    @DataBoundConstructor
    public UiPathSolutionDownloadPackage(String packageName,
                                         String destinationPath,
                                         SelectEntry credentials,
                                         TraceLevel traceLevel,
                                         String orchestratorAddress,
                                         String orchestratorTenant) {
        this.packageName = packageName;
        this.destinationPath = destinationPath;
        this.credentials = credentials;
        this.traceLevel = traceLevel;
        this.orchestratorAddress = orchestratorAddress;
        this.orchestratorTenant = orchestratorTenant;
    }

    @DataBoundSetter
    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    @DataBoundSetter
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public SelectEntry getCredentials() {
        return credentials;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public String getOrchestratorAddress() {
        return orchestratorAddress;
    }

    public String getOrchestratorTenant() {
        return orchestratorTenant;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        validateParameters();
        PrintStream logger = listener.getLogger();
        FilePath tempRemoteDir = tempDir(workspace);
        if (tempRemoteDir == null) {
            throw new AbortException("Failed to create temp folder for solution download.");
        }
        tempRemoteDir.mkdirs();
        Utility util = new Utility();
        try {
            EnvVars envVars = com.uipath.uipathpackage.util.TaskScopedEnvVarsManager.addRequiredEnvironmentVariables(run, env, listener);
            util.validateRuntime(launcher, envVars);
            CliDetails cliDetails = util.getCliDetails(run, listener, envVars, launcher);
            String buildTag = envVars.get("BUILD_TAG");

            FilePath expandedDestinationPath = destinationPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(destinationPath)) :
                    workspace.child(envVars.expand(destinationPath));
            expandedDestinationPath.mkdirs();

            SolutionDownloadPackageOptions options = new SolutionDownloadPackageOptions();
            if (cliDetails.getActualVersion().supportsNewTelemetry()) {
                options.populateAdditionalTelemetryData();
                options.setPipelineCorrelationId(buildTag);
                options.setCliGetFlow(cliDetails.getGetFlow());
            }
            options.setPackageName(envVars.expand(packageName));
            options.setDestinationPath(expandedDestinationPath.getRemote());
            if (packageVersion != null && !packageVersion.trim().isEmpty()) {
                options.setPackageVersion(envVars.expand(packageVersion));
            }
            if (fileName != null && !fileName.trim().isEmpty()) {
                options.setFileName(envVars.expand(fileName));
            }
            util.setCredentialsFromCredentialsEntry(credentials, options, run);
            options.setTraceLevel(traceLevel);
            options.setOrchestratorUrl(orchestratorAddress);
            options.setOrchestratorTenant(orchestratorTenant);

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String localization = country.isEmpty() ? language : language + "-" + country;
            options.setLanguage(localization);

            int exitCode = util.execute(
                    "SolutionDownloadPackageOptions",
                    options,
                    tempRemoteDir,
                    listener,
                    envVars,
                    launcher,
                    true
            );
            if (exitCode != 0) {
                throw new AbortException("UiPath CLI solution download-package failed with exit code: " + exitCode);
            }
            logger.println("Download package completed successfully.");
        } catch (URISyntaxException e) {
            e.printStackTrace(logger);
            throw new AbortException(e.getMessage());
        } finally {
            try {
                if (tempRemoteDir != null) tempRemoteDir.deleteRecursive();
            } catch (Exception e) {
                logger.println("Failed to delete temp folder after solution download: " + e.getMessage());
            }
        }
    }

    private void validateParameters() throws AbortException {
        Utility util = new Utility();
        util.validateParams(packageName, "Invalid solution package name");
        util.validateParams(destinationPath, "Invalid destination path");
        util.validateParams(orchestratorAddress, "Invalid orchestrator address");
        util.validateParams(orchestratorTenant, "Invalid orchestrator tenant");
        if (credentials == null) {
            throw new InvalidParameterException("Invalid credentials type");
        }
    }

    @Symbol("UiPathSolutionDownloadPackage")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "UiPath Solution: Download Package";
        }

        public FormValidation doCheckPackageName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) return FormValidation.error("Solution package name is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckDestinationPath(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) return FormValidation.error("Destination path is required.");
            if (value.toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error("Destination path must be inside the workspace.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckOrchestratorAddress(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("Orchestrator address is required.");
            return FormValidation.ok();
        }

        public FormValidation doCheckOrchestratorTenant(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("Orchestrator tenant is required.");
            return FormValidation.ok();
        }

        public List<Descriptor> getAuthenticationDescriptors() {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return new ArrayList<>();
            }
            List<Descriptor> list = new ArrayList<>();
            Descriptor userPass = jenkins.getDescriptor(UserPassAuthenticationEntry.class);
            if (userPass != null) list.add(userPass);
            Descriptor token = jenkins.getDescriptor(TokenAuthenticationEntry.class);
            if (token != null) list.add(token);
            Descriptor external = jenkins.getDescriptor(ExternalAppAuthenticationEntry.class);
            if (external != null) list.add(external);
            return ImmutableList.copyOf(list);
        }

        public ListBoxModel doFillTraceLevelItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            ListBoxModel result = new ListBoxModel();
            for (TraceLevel v : TraceLevel.values()) {
                result.add(v.toString(), v.toString());
            }
            return result;
        }
    }
}
