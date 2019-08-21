package com.uipath.uipathpackage;

import com.google.common.collect.ImmutableList;
import hudson.*;
import hudson.model.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static hudson.slaves.WorkspaceList.tempDir;

/**
 * Performs the actual build.
 */
public class UiPathPack extends Builder implements SimpleBuildStep {

    private final Entry version;
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
    public UiPathPack(Entry version, String projectJsonPath, String outputPath) {
        Utility util = new Utility();
        util.validateParams(projectJsonPath, "Invalid Project(s) Path");
        util.validateParams(outputPath, "Invalid Output Path");
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
        Utility util = new Utility();
        util.validateParams(projectJsonPath, "Invalid Project Json Path");
        util.validateParams(outputPath, "Invalid Output Path");
        FilePath tempRemoteDir = null;
        try {
            tempRemoteDir = tempDir(workspace);
            tempRemoteDir.mkdirs();
            FilePath tempOutputDir = tempRemoteDir.child("Output");
            tempOutputDir.mkdirs();
            EnvVars envVars = run.getEnvironment(listener);
            String importModuleCommands = util.importModuleCommands(tempRemoteDir, listener, envVars);
            String generatePackCommand;
            FilePath projectPath = new FilePath(new File(envVars.expand(projectJsonPath)));
            if (version instanceof ManualEntry) {
                String versionFormatted = util.escapePowerShellString(envVars.expand(((ManualEntry) version).getText().trim()));
                generatePackCommand = String.format("Pack -projectJsonPath %s -packageVersion %s -outputFolder %s", util.escapePowerShellString(projectPath.getRemote()), versionFormatted, util.escapePowerShellString(tempOutputDir.getRemote()));
            } else {
                generatePackCommand = String.format("Pack -projectJsonPath %s -outputFolder %s", util.escapePowerShellString(projectPath.getRemote()), util.escapePowerShellString(tempOutputDir.getRemote()));
            }
            util.command = util.getCommand(importModuleCommands, generatePackCommand);
            if(!util.execute(workspace, listener, envVars, launcher)){
                throw new AbortException("Failed to execute powershell session while importing the module and packing. Command : " + importModuleCommands + " " + generatePackCommand);
            }
            //copy result to outputDir
            FilePath outputDir = new FilePath(new File(envVars.expand(outputPath)));
            tempRemoteDir.child("Output").copyRecursiveTo(outputDir);
        } catch (URISyntaxException e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(e.getMessage());
        } finally {
            try {
                Objects.requireNonNull(tempRemoteDir).deleteRecursive();
            } catch (Exception e) {
                listener.getLogger().println("Failed to delete temp remote directory in UiPath Pack " + e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
        }
    }

    /**
     * Provide the project version
     *
     * @return Entry for versioning
     */
    public Entry getVersion() {
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

    /**
     * Partial default implementation of {@link Describable}.
     */
    public abstract static class Entry extends AbstractDescribableImpl<Entry> {
    }

    /**
     * Implementation of the auto versioning method
     */
    public static class AutoEntry extends Entry {

        /**
         * Blank Class to represent auto versioning
         */
        @DataBoundConstructor
        public AutoEntry() {
            //Do nothing because it is implementation for custom version, Hence doing nothing
        }

        @Symbol("AutoVersion")
        @Extension
        public static class DescriptorImpl extends Descriptor<Entry> {
            @Nonnull
            @Override
            public String getDisplayName() {
                return Messages.UiPathPack_AutoEntry_DescriptorImpl_DisplayName();
            }
        }
    }

    /**
     * Implementation of the Custom Versioning method, providing the text as input
     */
    public static class ManualEntry extends Entry {

        private final String text;

        /**
         * Custom Version as text
         *
         * @param text Custom version value
         */
        @DataBoundConstructor
        public ManualEntry(String text) {
            Utility util = new Utility();
            util.validateParams(text, "Invalid custom version");
            this.text = text;
        }

        /**
         * Getter for the text
         *
         * @return String custom version
         */
        public String getText() {
            return text;
        }

        /**
         * Metadata about a configurable instance.
         * <p>
         * {@link Descriptor} is an object that has metadata about a {@link Describable}
         * object, and also serves as a factory (in a way this relationship is similar
         * to {@link Object}/{@link Class} relationship.
         *
         * @see Describable
         */
        @Symbol("CustomVersion")
        @Extension
        public static class DescriptorImpl extends Descriptor<Entry> {
            @Nonnull
            @Override
            public String getDisplayName() {
                return Messages.UiPathPack_ManualEntry_DescriptorImpl_DisplayName();
            }
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
         * Provides the enlist of descriptors to the choice in hetero-radio
         *
         * @return list of the Entry descriptors
         */
        public List<Descriptor> getEntryDescriptors() {
            Jenkins jenkins = Jenkins.getInstance();
            List<Descriptor> list = new ArrayList<>();
            Descriptor autoDescriptor = jenkins.getDescriptor(AutoEntry.class);
            if (autoDescriptor != null) {
                list.add(autoDescriptor);
            }
            Descriptor manualDescriptor = jenkins.getDescriptor(ManualEntry.class);
            if (manualDescriptor != null) {
                list.add(manualDescriptor);
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
                return FormValidation.error(Messages.UiPathPack_DescriptorImpl_errors_missingProjectJsonPath());
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
                return FormValidation.error(Messages.UiPathPack_DescriptorImpl_error_missingOutputPath());
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
            return Messages.UiPathPack_DescriptorImpl_DisplayName();
        }
    }
}
