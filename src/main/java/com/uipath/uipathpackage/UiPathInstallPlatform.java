package com.uipath.uipathpackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uipath.uipathpackage.configuration.UiPathCliConfiguration;
import com.uipath.uipathpackage.util.TaskScopedEnvVarsManager;
import com.uipath.uipathpackage.util.TraceLevel;
import com.uipath.uipathpackage.util.Utility;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UiPathInstallPlatform extends Builder implements SimpleBuildStep {

    private final Utility util;

    private String cliVersion;

    private String cliNupkgPath;

    private boolean forceInstall;

    private final TraceLevel traceLevel;

    private static final UiPathCliConfiguration cliConfiguration;

    static {
        try {
            cliConfiguration = UiPathCliConfiguration.getInstance();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @DataBoundConstructor
    public UiPathInstallPlatform(String cliNupkgPath, TraceLevel traceLevel) {
        this.util = new Utility();
        this.cliVersion = cliConfiguration.getDefaultCliVersionKey();
        this.cliNupkgPath = cliNupkgPath;
        this.forceInstall = false;
        this.traceLevel = traceLevel;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener) throws AbortException {
        PrintStream logger = listener.getLogger();
        try {
            EnvVars envVars = TaskScopedEnvVarsManager.addRequiredEnvironmentVariables(run, env, listener);

            String scopedVersion = cliVersion;
            UiPathCliConfiguration.Configuration versionConfiguration = null;
            FilePath actualCliNupkgPath = null;

            if (scopedVersion.contains("CustomVersion")) {
                if(StringUtils.isBlank(cliNupkgPath)){
                    throw new AbortException("CustomVersion is selected, but path to local nupkg is not provided.");
                }
                try {
                    actualCliNupkgPath = cliNupkgPath.contains("${WORKSPACE}") ?
                            new FilePath(launcher.getChannel(), envVars.expand(cliNupkgPath)) :
                            workspace.child(envVars.expand(cliNupkgPath));

                    versionConfiguration = cliConfiguration.getConfigurationFromPackageName(actualCliNupkgPath.getBaseName());
                    scopedVersion = versionConfiguration.getConfigurationUniqueId();
                    cliConfiguration.AddCliConfiguration(scopedVersion, versionConfiguration);
                } catch (Exception e) {
                    logger.println("Exception: " + e.getMessage());
                    throw new AbortException("Failed to parse custom CLI version from path: " + cliNupkgPath + ". Make sure you didn't change default nupkg name downloaded from feed");
                }
            }
            else {
                versionConfiguration = cliConfiguration.getConfiguration(scopedVersion);
            }

            logger.println("Validating CLI selected version: " + versionConfiguration.getDisplayName() + "...");
            validateCliCompatibility(versionConfiguration, logger, workspace);

            boolean isSelectedCliAlreadyCached = cliConfiguration.getCliPath(launcher, envVars, scopedVersion).isPresent();

            logger.println(isSelectedCliAlreadyCached ? "cli " + scopedVersion + " is already cached.." : "cli " + scopedVersion + " is not found in cache..");

            if(this.forceInstall || !isSelectedCliAlreadyCached) {
                if(forceInstall) {
                    logger.println("force installing the cli , any previous cache for version " + scopedVersion + " will be invalidated..");
                }

                FilePath cliRootCacheDirPath = cliConfiguration.getCliRootCachedDirectoryPath(launcher, envVars, scopedVersion);

                if(scopedVersion.equals(cliConfiguration.getDefaultCliVersionKey())) {
                    logger.print("(caching) extracting the pre-packaged cli...");
                    util.extractCliApp(cliRootCacheDirPath, listener, envVars);

                } else if(cliVersion.contains("CustomVersion") && StringUtils.isNotBlank(cliNupkgPath)) {
                    if(!actualCliNupkgPath.exists()){
                        logger.println("CliNupkgPath provided doesn't exists "+actualCliNupkgPath.getRemote());
                        throw new AbortException(Messages.UiPathInstallPlatform_DescriptorImpl_Error_CliNupkgPath());
                    }
                    logger.println("(caching) extracting provided cli-nuget from path " + actualCliNupkgPath.getRemote());
                    actualCliNupkgPath.unzip(cliRootCacheDirPath);
                } else {
                    UiPathCliConfiguration.Configuration configuration = cliConfiguration.getConfiguration().get(scopedVersion);
                    FilePath downloadsRootPath = cliConfiguration.getCliRootDownloadsDirectoryPath(launcher, envVars, scopedVersion);

                    String fileName = configuration.getName().concat(".").concat(configuration.getVersion().getComplete()).concat(".nupkg");

                    FilePath downloadCliPath = downloadsRootPath.child(fileName);
                    util.downloadCli(configuration.getFeedUrl(), downloadCliPath, listener);

                    logger.println("(caching) extracting the downloaded cli...");
                    downloadCliPath.unzip(cliRootCacheDirPath);
                }
                logger.println("Finished extraction for UipCLI version: " + scopedVersion);
            }

            cliConfiguration.updateSelectedCliVersionKey(run, scopedVersion);
        } catch (Exception e) {
            if(traceLevel.equals(TraceLevel.Verbose) || traceLevel.equals(TraceLevel.Error)) {
                e.printStackTrace(logger);
            }
            throw new AbortException("unable to install the cli "+ e.getMessage());
        }
    }

    private void validateCliCompatibility(UiPathCliConfiguration.Configuration cliSelectedVersion, PrintStream logger, FilePath workspace) throws AbortException {
        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException("Unable to determine the operating system of the agent.");
        }

        String osName = null;
        try {
            osName = computer.getSystemProperties().get("os.name").toString().toLowerCase();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.println("Detected OS: " + osName);

        if (osName.contains("win")) {
            if (cliSelectedVersion.getPlatform() != UiPathCliConfiguration.CliPlatform.Windows) {
                throw new AbortException("Selected UiPath CLI version '" + cliSelectedVersion.getDisplayName() + "' cannot be executed on Windows agent.");
            }
        } else if (osName.contains("linux")) {
            if (cliSelectedVersion.getPlatform() != UiPathCliConfiguration.CliPlatform.Linux) {
                throw new AbortException("Selected UiPath CLI version '" + cliSelectedVersion.getDisplayName() + "' cannot be executed on Linux agent.");
            }
        } else {
            throw new AbortException("Running on incompatible operating system");
        }
    }

    @DataBoundSetter
    public void setCliVersion(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    @DataBoundSetter
    public void setCliNupkgPath(String cliNupkgPath) {
        this.cliNupkgPath = cliNupkgPath;
    }

    @DataBoundSetter
    public void setForceInstall(boolean forceInstall) {
        this.forceInstall = forceInstall;
    }

    public String getCliVersion() {
        return cliVersion;
    }

    public String getCliNupkgPath() {
        return cliNupkgPath;
    }

    public boolean isForceInstall() {
        return forceInstall;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    @Symbol("UiPathInstallPlatform")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Returns true if this task is applicable to the given project.
         *
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see AbstractProject.AbstractProjectDescriptor#isApplicable(Descriptor)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return com.uipath.uipathpackage.Messages.UiPathInstallPlatform_DescriptorImpl_DisplayName();
        }

        public FormValidation doCheckCliNupkgPath(@QueryParameter String value) {
            if (StringUtils.isNotBlank(value) && value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillCliVersionItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            ListBoxModel result= new ListBoxModel();

            List<Map.Entry<String, UiPathCliConfiguration.Configuration>> entries =
                    new ArrayList<>(cliConfiguration.getConfiguration().entrySet());
            entries.sort(Comparator.comparing((Map.Entry<String, UiPathCliConfiguration.Configuration> e)
                            -> e.getValue().getName())
                    .thenComparing(e -> e.getValue().getVersion().getComplete(), Comparator.reverseOrder()));

           for (Map.Entry<String, UiPathCliConfiguration.Configuration> v : entries) {
               result.add(new ListBoxModel.Option(v.getValue().getDisplayName(), v.getKey()));
           }

           return result;
        }

        public ListBoxModel doFillTraceLevelItems(@AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            ListBoxModel result= new ListBoxModel();
            for (TraceLevel v: TraceLevel.values()) {
                result.add(v.toString(), v.toString());
            }

            return result;
        }
    }
}
