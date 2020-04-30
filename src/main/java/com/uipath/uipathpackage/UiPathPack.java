package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.versioning.AutoVersionEntry;
import com.uipath.uipathpackage.entries.versioning.CurrentVersionEntry;
import com.uipath.uipathpackage.entries.versioning.ManualVersionEntry;
import com.uipath.uipathpackage.models.PackOptions;
import com.uipath.uipathpackage.util.Utility;
import hudson.*;
import hudson.model.*;
import hudson.remoting.Channel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Performs the actual build.
 */
public class UiPathPack extends Builder implements SimpleBuildStep {
    private final Utility util = new Utility();
    private final SelectEntry version;
    private final String projectJsonPath;
    private final String outputPath;

    /**
     * Data bound constructor responsible for setting the values param values to state
     *
     * @param version         Entry version
     * @param projectJsonPath Project Json Path
     * @param outputPath      Output Path
     */
    @DataBoundConstructor
    public UiPathPack(SelectEntry version, String projectJsonPath, String outputPath) {
        this.version = version;
        this.projectJsonPath = projectJsonPath;
        this.outputPath = outputPath;
    }

    /**
     * Run this step.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        validateParameters();

        FilePath tempRemoteDir = tempDir(workspace);
        tempRemoteDir.mkdirs();

        try {
            EnvVars envVars = run.getEnvironment(listener);

            FilePath expandedOutputPath = outputPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(outputPath)) :
                    workspace.child(envVars.expand(outputPath));
            expandedOutputPath.mkdirs();

            FilePath expandedProjectJsonPath = projectJsonPath.contains("${WORKSPACE}") ?
                    new FilePath(launcher.getChannel(), envVars.expand(projectJsonPath)) :
                    workspace.child(envVars.expand(projectJsonPath));

            PackOptions packOptions = new PackOptions();

            packOptions.setDestinationFolder(expandedOutputPath.getRemote());
            packOptions.setProjectPath(expandedProjectJsonPath.getRemote());

            if (version instanceof ManualVersionEntry) {
                packOptions.setVersion(envVars.expand(((ManualVersionEntry) version).getVersion().trim()));
            } else if (version instanceof AutoVersionEntry) {
                packOptions.setAutoVersion(true);
            }

            int result = util.execute("pack", packOptions, tempRemoteDir, listener, envVars, launcher);
            if (result != 0) {
                throw new AbortException("Failed to run the command");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
        } finally {
            try{
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            }catch(Exception e){
                listener.getLogger().println("Failed to delete temp remote directory in UiPath Pack "+ e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
        }
    }

    /**
     * Provide the project version
     *
     * @return Entry for versioning
     */
    public SelectEntry getVersion() {
        return version;
    }

    /**
     * Provides the project json path
     *
     * @return String projectJsonPath
     */
    public String getProjectJsonPath() {
        return projectJsonPath;
    }

    /**
     * Provides the Output Path
     *
     * @return String outputPath
     */
    public String getOutputPath() {
        return outputPath;
    }

    private void validateParameters() throws AbortException {
        if (version == null)
        {
            throw new InvalidParameterException(com.uipath.uipathpackage.Messages.GenericErrors_MissingVersioningMethod());
        }

        util.validateParams(projectJsonPath, "Invalid Project(s) Path");
        util.validateParams(outputPath, "Invalid Output Path");

        if (outputPath.toUpperCase().contains("${JENKINS_HOME}")) {
            throw new AbortException("Paths containing JENKINS_HOME are not allowed, use the Archive Artifacts plugin to copy the required files to the build output.");
        }
    }

    /**
     * {@link Descriptor} for {@link Builder}
     */
    @Symbol("UiPathPack")
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

        /**
         * Provides the list of descriptors to the choice in hetero-radio
         *
         * @return list of the Entry descriptors
         */
        public List<Descriptor> getEntryDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();
            Descriptor autoDescriptor = jenkins.getDescriptor(AutoVersionEntry.class);
            if (autoDescriptor != null) {
                list.add(autoDescriptor);
            }
            Descriptor manualDescriptor = jenkins.getDescriptor(ManualVersionEntry.class);
            if (manualDescriptor != null) {
                list.add(manualDescriptor);
            }
            Descriptor currentEntryDescriptor = jenkins.getDescriptor(CurrentVersionEntry.class);
            if (currentEntryDescriptor != null) {
                list.add(currentEntryDescriptor);
            }
            return ImmutableList.copyOf(list);
        }

        /**
         * Validated the Project(s) path
         *
         * @param value Project Json Path value
         * @return FormValidation
         */
        public FormValidation doCheckProjectJsonPath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_Error_MissingProjectJsonPath());
            }

            if (value.trim().toUpperCase().contains("${JENKINS_HOME}")) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.GenericErrors_MustUseSlavePaths());
            }

            return FormValidation.ok();
        }

        /**
         * Validates the output path
         *
         * @param value Output Path value
         * @return FormValidation
         */
        public FormValidation doCheckOutputPath(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error(com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_Error_MissingOutputPath());
            }
            return FormValidation.ok();
        }

        /**
         * Provides the display name to the build step
         *
         * @return String display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return com.uipath.uipathpackage.Messages.UiPathPack_DescriptorImpl_DisplayName();
        }
    }
}
