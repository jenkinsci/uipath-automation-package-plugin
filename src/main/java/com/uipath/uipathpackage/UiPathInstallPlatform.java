package com.uipath.uipathpackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uipath.uipathpackage.configuration.CliConfiguration;
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
import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

public class UiPathInstallPlatform extends Builder implements SimpleBuildStep {

    private final Utility util;

    private String cliVersion;

    private String cliNupkgPath;

    private boolean forceInstall;

    private final TraceLevel traceLevel;

    private static final CliConfiguration cliConfiguration;

    static {
        try {
            cliConfiguration = CliConfiguration.getInstance();
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
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws AbortException {
        String defaultVersion = cliConfiguration.getDefaultCliVersionKey();
        PrintStream logger = listener.getLogger();

        System.setProperty(CliConfiguration.GLOBAL_SELECTED_CLI_VERSION_KEY, cliVersion);

        try {
            boolean isSelectedCliAlreadyCached = cliConfiguration.getCliPath(launcher, env, cliVersion).isPresent();

            logger.println(isSelectedCliAlreadyCached ? "cli is already cached.." : "cli is not found in cache..");

            if(this.forceInstall || !isSelectedCliAlreadyCached) {

                if(forceInstall) {
                    logger.println("force installing the cli , any previous cache for version "+cliVersion+" will be invalidate..");
                }

                Optional<FilePath> cliRootCacheDirPath = cliConfiguration.getCliRootCachedDirectoryPath(launcher, env, cliVersion);

                if(!cliRootCacheDirPath.isPresent()) {
                    logger.println("invalid cli configuration might have caused this issue.");
                    throw new AbortException("invalid cli configuration might have caused this issue.");
                }

                if(cliVersion.equals(defaultVersion)) {
                    logger.print("(caching) extracting the pre-packaged cli...");
                    util.extractCliApp(cliRootCacheDirPath.get(), listener, env);
                } else if(StringUtils.isNotBlank(cliNupkgPath)) {

                    FilePath actualCliNupkgPath = cliNupkgPath.contains("${WORKSPACE}") ?
                            new FilePath(launcher.getChannel(), env.expand(cliNupkgPath)) :
                            workspace.child(env.expand(cliNupkgPath));

                    if(!actualCliNupkgPath.exists()){
                        logger.println("CliNupkgPath provided doesn't exists "+actualCliNupkgPath.getRemote());
                        throw new AbortException(Messages.UiPathInstallPlatform_DescriptorImpl_Error_CliNupkgPath());
                    }
                    logger.print("(caching) extracting the provided cli-nuget...");
                    actualCliNupkgPath.unzip(cliRootCacheDirPath.get());
                } else {
                    CliConfiguration.Configuration configuration = cliConfiguration.getConfiguration().get(cliVersion);
                    Optional<FilePath> downloadsRootPath = cliConfiguration.getCliRootDownloadsDirectoryPath(launcher, env, cliVersion);
                    if(!downloadsRootPath.isPresent()) {
                        logger.println("(downloadsRootPath) invalid cli configuration might have caused this issue.");
                        throw new AbortException("invalid cli configuration might have caused this issue.");
                    }
                    String fileName = configuration.getName().concat(".").concat(configuration.getVersion().getComplete()).concat(".nupkg");
                    downloadsRootPath.get().child(fileName);

                    FilePath downloadCliPath = new FilePath(new File(downloadsRootPath.get().getRemote()));
                    util.downloadCli(configuration.getFeedUrl(), downloadCliPath, listener);

                    logger.print("(caching) extracting the downloaded cli...");
                    downloadCliPath.unzip(cliRootCacheDirPath.get());
                }
                logger.println(" done!!");
            }
        } catch (Exception e) {
            if(traceLevel.equals(TraceLevel.Verbose) || traceLevel.equals(TraceLevel.Error)) {
                e.printStackTrace(logger);
            }
            throw new AbortException("unable to install the cli "+e.getMessage());
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

            for (Map.Entry<String, CliConfiguration.Configuration> v: cliConfiguration.getConfiguration().entrySet()) {
                result.add(v.getValue().getDisplayName(), v.getKey());
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
