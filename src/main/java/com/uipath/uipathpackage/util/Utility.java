package com.uipath.uipathpackage.util;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.job.DynamicallyEntry;
import com.uipath.uipathpackage.entries.job.RobotEntry;
import com.uipath.uipathpackage.models.AuthenticatedOptions;
import com.uipath.uipathpackage.models.JobOptions;
import com.uipath.uipathpackage.models.RunOptions;
import com.uipath.uipathpackage.models.SerializableCliOptions;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Utility Class used by UiPathDeploy and UiPathPack
 */
public class Utility {
    /**
     * Validates the param for null or empty check
     *
     * @param param Param to validate
     * @param s     Error Message
     */
    public void validateParams(@Nonnull String param, @Nonnull String s) {
        if (param.trim().isEmpty()) {
            throw new InvalidParameterException(s);
        }
    }

    /***
     * Returns the bundle value of config
     * @param rb ResourceBundle
     * @param s key of the bundle
     * @return value of the resource bundle corresponding to key
     */
    public String getConfigValue(@Nonnull ResourceBundle rb, @Nonnull String s) {
        return rb.getString(s);
    }

    public int execute(@Nonnull String command, @Nonnull SerializableCliOptions options, @Nonnull FilePath remoteTempDir, @Nonnull TaskListener listener, @Nonnull EnvVars envVars, @Nonnull Launcher launcher, boolean throwExceptionOnFailure) throws IOException, InterruptedException, URISyntaxException {
        if (remoteTempDir.getRemote().toUpperCase().contains(":\\WINDOWS\\SYSTEM32")) {
            throw new AbortException("The plugin cannot be executed in a workspace path inside the WINDOWS folder. Please use a custom workspace folder that is outside the WINDOWS folder for this build definition or reinstall Jenkins and use a local user account instead.");
        }

        FilePath cliPath = extractCliApp(remoteTempDir, listener, envVars);
        FilePath commandOptionsFile = remoteTempDir.createTextTempFile("uipcliargs", "", new JSONObject(new RunOptions(command, options)).toString());

        int result = launcher.launch().cmds(this.buildCommandLine(cliPath, commandOptionsFile)).envs(envVars).stdout(listener).pwd(cliPath.getParent()).start().join();
        if (throwExceptionOnFailure && result != 0) {
            throw new AbortException("Failed to run the command, the CLI failed with error code " + result);
        }

        return result;
    }


    public FilePath extractCliApp(@Nonnull FilePath tempRemoteDir, @Nonnull TaskListener listener, @Nonnull EnvVars env) throws IOException, InterruptedException, URISyntaxException {
        PrintStream logger = listener.getLogger();
        ResourceBundle rb = ResourceBundle.getBundle("config");
        String cliFolderName = "cli-" + this.getConfigValue(rb, "UiPath.CLI.Version");
        FilePath targetCliPath = tempRemoteDir.child(cliFolderName).child("lib").child("net461").child("uipcli.exe");

        if (targetCliPath.exists())
        {
            logger.println("Using previously extracted UiPath CLI from " + targetCliPath);
            return targetCliPath;
        }

        String pluginJarPath;

        if (isServerOSWindows()) {
            pluginJarPath = env.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar");
        } else {
            pluginJarPath = env.expand("${JENKINS_HOME}/plugins/uipath-automation-package/WEB-INF/lib/uipath-automation-package.jar");
        }

        logger.println("Expected plugin jar path on Jenkins master: " + pluginJarPath + ", extracting...");

        // Copy relevant files to temp directory
        copyPluginFilesToTempDir(listener, tempRemoteDir, pluginJarPath);
        return targetCliPath;
    }

    public void setCredentialsFromCredentialsEntry(SelectEntry credentials, AuthenticatedOptions options, @Nonnull Run<?, ?> run) throws AbortException {
        if (credentials instanceof UserPassAuthenticationEntry) {
            StandardUsernamePasswordCredentials cred = CredentialsProvider.findCredentialById(((UserPassAuthenticationEntry) credentials).getCredentialsId(), StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
            if (cred == null || cred.getUsername().isEmpty() || cred.getPassword().getPlainText().isEmpty()) {
                throw new AbortException("Invalid credentials");
            }

            options.setUsername(cred.getUsername());
            options.setPassword(cred.getPassword().getPlainText());
        } else {
            StringCredentials cred = CredentialsProvider.findCredentialById(((TokenAuthenticationEntry) credentials).getCredentialsId(), StringCredentials.class, run, Collections.emptyList());
            if (cred == null || cred.getSecret().getPlainText().isEmpty()) {
                throw new AbortException("Invalid credentials");
            }

            options.setRefreshToken(cred.getSecret().getPlainText());
            options.setAccountName(((TokenAuthenticationEntry) credentials).getAccountName());
        }
    }

    public void setJobRunFromStrategyEntry(SelectEntry strategy, JobOptions options) {
        if (strategy == null)
        {
            options.setJobsCount(1);
            options.setUser("");
            options.setMachine("");

            return;
        }

        if (strategy instanceof DynamicallyEntry) {
            options.setJobsCount(((DynamicallyEntry) strategy).getJobsCount());
            options.setUser(((DynamicallyEntry) strategy).getUser());
            options.setMachine(((DynamicallyEntry) strategy).getMachine());
            options.setRobots(new String[]{});
        }else {
            String robotNames = ((RobotEntry) strategy).getRobotsIds();
            if (robotNames != null)
            {
                options.setRobots(robotNames.split(","));
            }
            else
            {
                options.setRobots(new String[]{});
            }
        }
    }

    private String[] buildCommandLine(FilePath cliPath, FilePath commandOptionsFile) {
        return new String[] { cliPath.getRemote(), "run", commandOptionsFile.getRemote() };
    }

    private void extractResourcesToTempFolder(FilePath tempDir, File jarfile, TaskListener listener) throws IOException, InterruptedException {
        try (JarFile archive = new JarFile(jarfile)) {
            // sort entries by name to always create folders first
            List<? extends JarEntry> entries = archive.stream().sorted(Comparator.comparing(JarEntry::getName)).collect(Collectors.toList());
            for (JarEntry entry : entries) {
                if (!entry.getName().startsWith("cli")) {
                    continue;
                }

                FilePath destination = tempDir.child(entry.getName());

                if (entry.isDirectory()) {
                    destination.mkdirs();
                } else {
                    destination.copyFrom(archive.getInputStream(entry));
                }
            }
        } catch (JarException e) {
            e.printStackTrace(listener.getLogger());
            throw e;
        }
    }

    private void copyPluginFilesToTempDir(@Nonnull TaskListener listener, FilePath tempDir, String pluginJarPath) throws IOException, URISyntaxException, InterruptedException {
        File jar = new File(pluginJarPath);
        if (!jar.exists()) {
            // For snapshot plugin dependencies, an IDE may have replaced ~/.m2/repository/…/${artifactId}.hpi with …/${artifactId}-plugin/target/classes/
            // which unfortunately lacks META-INF/MANIFEST.MF so try to find index.jelly (which every plugin should include) and thus the ${artifactId}.hpi:
            Enumeration<URL> jellies = getClass().getClassLoader().getResources("index.jelly");
            while (jellies.hasMoreElements()) {
                URL jellyU = jellies.nextElement();
                if (jellyU.getProtocol().equals("file")) {
                    File jellyF = new File(jellyU.toURI());
                    File classes = jellyF.getParentFile();
                    if (classes.getName().equals("classes")) {
                        FilePath wsClasses = new FilePath(classes);
                        wsClasses.copyRecursiveTo(tempDir);
                        listener.getLogger().println("Plugin files copied to temp");
                    }
                }
            }
        } else {
            listener.getLogger().print("Extracting cli to temp folder " + tempDir.getRemote() + "...");
            extractResourcesToTempFolder(tempDir, jar, listener);
            listener.getLogger().println("done!" );
        }
    }

    private boolean isServerOSWindows() {
        return System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH).contains("win");
    }


}
