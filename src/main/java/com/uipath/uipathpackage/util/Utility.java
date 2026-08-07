package com.uipath.uipathpackage.util;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.uipath.uipathpackage.configuration.UiPathCliConfiguration;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.entries.authentication.ExternalAppAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.TokenAuthenticationEntry;
import com.uipath.uipathpackage.entries.authentication.UserPassAuthenticationEntry;
import com.uipath.uipathpackage.entries.job.*;
import com.uipath.uipathpackage.models.*;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.uipath.uipathpackage.configuration.UiPathCliConfiguration.DEFAULT_CLI_VERSION_KEY;
import hudson.Util;

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

        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();
        Optional<FilePath> cachedCliPath = cliConfiguration.getCliPath(launcher, envVars, cliConfiguration.getSelectedOrDefaultCliVersionKey(envVars));

        FilePath cliPath;
        if (cachedCliPath.isPresent()) {
            cliPath = cachedCliPath.get();
        } else {
            FilePath cliRootCacheDirPath = cliConfiguration.getCliRootCachedDirectoryPath(launcher, envVars, cliConfiguration.getDefaultCliVersionKey());
            cliPath = extractCliApp(cliRootCacheDirPath, listener, envVars);
        }

        FilePath commandOptionsFile = remoteTempDir.createTextTempFile("uipcliargs", "", new JSONObject(new RunOptions(command, options)).toString());

        String[] commandParams = new String[]{"dotnet", cliPath.getRemote(), "run", commandOptionsFile.getRemote()};
        int result = launcher.launch().cmds(commandParams).envs(envVars).stdout(listener).pwd(cliPath.getParent()).start().join();
        if (throwExceptionOnFailure && result != 0) {
            throw new AbortException("Failed to run the command, the CLI failed with error code " + result);
        }

        return result;
    }

    public  CliDetails getCliDetails(
            @Nonnull Run<?, ?> run,
            @Nonnull TaskListener listener,
            @Nonnull EnvVars envVars,
            @Nonnull Launcher launcher) throws IOException, InterruptedException, URISyntaxException {
        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();

        String version = cliConfiguration.getSelectedOrDefaultCliVersionKey(envVars);
        launcher.getListener().getLogger().println("Selected CLI version: " + envVars.get(UiPathCliConfiguration.SELECTED_CLI_VERSION_KEY));
        launcher.getListener().getLogger().println("Used CLI version: " + version);
        Optional<FilePath> cachedCliPath = cliConfiguration.getCliPath(launcher, envVars, version);

        FilePath cliPath;
        CliGetFlow cliGetFlow;
        if(cachedCliPath.isPresent()) {
            cliPath = cachedCliPath.get();
            cliGetFlow = CliGetFlow.CachedTool;
        } else {
            FilePath cliRootCacheDirPath = cliConfiguration.getCliRootCachedDirectoryPath(launcher, envVars, cliConfiguration.getDefaultCliVersionKey());
            cliPath = extractCliApp(cliRootCacheDirPath, listener, envVars);
            cliGetFlow = CliGetFlow.ExtractDefaultCli;
        }

        ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

        StreamTaskListener execListener = new StreamTaskListener(commandOutput, run.getCharset());

        String [] commandParameters = new String[] {"dotnet", cliPath.getRemote(), "--version" };
        launcher.launch().cmds(commandParameters).envs(envVars).stdout(execListener).pwd(cliPath.getParent()).start().join();

        String stdoutText = commandOutput.toString(run.getCharset().name());

        CliDetails response = new CliDetails();
        response.setActualVersion(extractActualVersionFromText(stdoutText));
        response.setGetFlow(cliGetFlow);
        return response;
    }

    private ActualVersion extractActualVersionFromText(String text) {
        Pattern versionPattern = Pattern.compile("uipcli (\\d+)\\.(\\d+)\\.\\d+");

        Matcher matcher = versionPattern.matcher(text);

        if (matcher.find()) {
            int majorVersion = Integer.parseInt(matcher.group(1));
            int minorVersion = Integer.parseInt(matcher.group(2));

            return new ActualVersion(majorVersion, minorVersion);
        } else {
            return new ActualVersion(-1, -1);
        }
    }

    public void validateRuntime(@Nonnull Launcher launcher, @Nonnull EnvVars envVars) throws AbortException, JsonProcessingException {
        UiPathCliConfiguration configuration = UiPathCliConfiguration.getInstance();
        String selectedCliVersionKey = configuration.getSelectedOrDefaultCliVersionKey(envVars);
        UiPathCliConfiguration.Configuration cliConfig = configuration.getConfiguration().get(selectedCliVersionKey);

        if (launcher.isUnix() && cliConfig.getWindowsCompatible()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseLinux());
        }
    }

    public FilePath extractCliApp(@Nonnull FilePath targetRootCacheDir, @Nonnull TaskListener listener, @Nonnull EnvVars env) throws IOException, InterruptedException, URISyntaxException {
        PrintStream logger = listener.getLogger();
        FilePath targetCliPath = getDotnetToolCliPath(targetRootCacheDir);

        if (targetCliPath.exists())
        {
            logger.println("Using previously extracted UiPath CLI from " + targetCliPath);
            return targetCliPath;
        }

        String pluginJarPath = getPluginJarPath();

        logger.println("Expected plugin jar path on Jenkins master: " + pluginJarPath + ", extracting...");

        // Copy relevant files to temp directory
        copyPluginFilesToTempDir(listener, targetRootCacheDir, pluginJarPath);
        return targetCliPath;
    }

    /**
     * Gets the plugin jar path using Jenkins PluginManager API, which respects custom workDir configuration.
     * The baseResourceURL automatically uses workDir when configured via hudson.PluginManager.workDir.
     *
     * @return The absolute path to the plugin jar file
     * @throws IOException if the plugin or jar file cannot be found
     */
    private String getPluginJarPath() throws IOException {
        jenkins.model.Jenkins jenkinsInstance = jenkins.model.Jenkins.get();
        hudson.PluginWrapper pluginWrapper = jenkinsInstance.getPluginManager().getPlugin("uipath-automation-package");

        if (pluginWrapper == null) {
            throw new IOException("Could not find uipath-automation-package plugin in Jenkins PluginManager");
        }

        if (pluginWrapper.baseResourceURL == null) {
            throw new IOException("Plugin baseResourceURL is null - plugin may not be properly initialized");
        }

        File pluginBaseDir;
        try {
            pluginBaseDir = new File(pluginWrapper.baseResourceURL.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Failed to convert plugin baseResourceURL to URI: " + e.getMessage(), e);
        }

        File jarFile = new File(pluginBaseDir, "WEB-INF/lib/uipath-automation-package.jar");
        if (!jarFile.exists()) {
            throw new IOException("Plugin jar file not found at expected location: " + jarFile.getAbsolutePath());
        }

        return jarFile.getAbsolutePath();
    }

    // With support for .NET tool structure, look for uipcli.dll in tools/netX.X/any/uipcli.dll. Maintain also backward compatibility.
    public static FilePath getDotnetToolCliPath(FilePath targetPath) throws IOException, InterruptedException {
        FilePath uipcliToolPath;

        FilePath toolsDir = targetPath.child("tools");
        uipcliToolPath = toolsDir.child("uipcli.dll");

        if(uipcliToolPath.exists()) {
            return uipcliToolPath;
        }

        if (toolsDir.exists()) {
            for (FilePath child : toolsDir.listDirectories()) {
                String dirName = child.getName();
                if (dirName.startsWith("net")) {
                    uipcliToolPath = child.child("any").child("uipcli.dll");
                    break;
                }
            }
        }
        return uipcliToolPath;
    }

    public void downloadCli(String feedUrl,@Nonnull FilePath downloadPath, @Nonnull TaskListener listener) throws AbortException {
        PrintStream logger = listener.getLogger();
        logger.println("Downloading CLI from "+ feedUrl);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(feedUrl))
                .header("Accept", "application/octet-stream, */*")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();
            if (status < 200 || status >= 300) {
                String resp = body != null ? new String(body, StandardCharsets.UTF_8) : "";
                logger.println("Aborting Task Unable to Download CLI.... HttpStatus " + status + " Response " + resp + " Error HTTP " + status);
                throw new AbortException("unable to download the CLI from the public feed");
            }
            try (ByteArrayInputStream in = new ByteArrayInputStream(body)) {
                downloadPath.copyFrom(in);
            } catch (InterruptedException ie) {
                ie.printStackTrace(logger);
                throw new AbortException("Error while writing nupkg to download directory ");
            }
            logger.println("Downloaded CLI successfully. @"+ downloadPath);
        } catch (InterruptedException ie) {
            ie.printStackTrace(logger);
            Thread.currentThread().interrupt();
            logger.println("Aborting Task Unable to Download CLI.... Error "+ ie.getMessage() + " Download Path "+downloadPath.getRemote());
            throw new AbortException("unable to download the CLI from the public feed");
        } catch (IOException ioe) {
            logger.println("Aborting Task Unable to Download CLI.... Error "+ ioe.getMessage() + " Download Path "+downloadPath.getRemote());
            throw new AbortException("unable to download the CLI from the public feed");
        }
    }

    public void setCredentialsFromCredentialsEntry(SelectEntry credentials, AuthenticatedOptions options, @Nonnull Run<?, ?> run) throws AbortException {
        if (credentials instanceof UserPassAuthenticationEntry) {
            StandardUsernamePasswordCredentials cred = CredentialsProvider.findCredentialById(((UserPassAuthenticationEntry) credentials).getCredentialsId(), StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
            if (cred == null || cred.getUsername().isEmpty() || cred.getPassword().getPlainText().isEmpty()) {
                throw new AbortException("Invalid credentials");
            }

            options.setUsername(cred.getUsername());
            options.setPassword(cred.getPassword().getPlainText());
        } else if (credentials instanceof TokenAuthenticationEntry) {
            StringCredentials cred = CredentialsProvider.findCredentialById(((TokenAuthenticationEntry) credentials).getCredentialsId(), StringCredentials.class, run, Collections.emptyList());
            if (cred == null || cred.getSecret().getPlainText().isEmpty()) {
                throw new AbortException("Invalid credentials");
            }

            options.setRefreshToken(cred.getSecret().getPlainText());
            options.setAccountName(((TokenAuthenticationEntry) credentials).getAccountName());
        } else {
            StringCredentials secret = CredentialsProvider.findCredentialById(((ExternalAppAuthenticationEntry) credentials).getApplicationSecret(), StringCredentials.class, run, Collections.emptyList());
            if (secret == null || secret.getSecret().getPlainText().isEmpty()) {
                throw new AbortException("Invalid credentials");
            }

            ExternalAppAuthenticationEntry cred = (ExternalAppAuthenticationEntry) credentials;
            options.setAccountForApp(cred.getAccountForApp());
            options.setApplicationId(cred.getApplicationId());
            options.setApplicationSecret(secret.getSecret().getPlainText());
            if (Util.fixEmptyAndTrim(cred.getApplicationScope()) != null) {
                options.setApplicationScope(cred.getApplicationScope());
            }
            if (Util.fixEmptyAndTrim(cred.getIdentityUrl()) != null) {
            	options.setAuthorizationUrl(cred.getIdentityUrl());
            }
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

    public void setJobRunFromJobTypeEntry(SelectEntry strategy, JobOptions options) {
        if (strategy instanceof NonProductionJobTypeEntry) {
            options.setJobType(JobType.NonProduction);
        }
        else if (strategy instanceof UnattendedJobTypeEntry)
        {
            options.setJobType(JobType.Unattended);
        }else if (strategy instanceof TestAutomationJobTypeEntry){
        	options.setJobType(JobType.TestAutomation);
        }
    }

    private void extractResourcesToTempFolder(FilePath tempDir, File jarfile, TaskListener listener) throws IOException, InterruptedException {
        try (JarFile archive = new JarFile(jarfile)) {
            // sort entries by name to always create folders first
            List<? extends JarEntry> entries = archive.stream().sorted(Comparator.comparing(JarEntry::getName)).collect(Collectors.toList());
            for (JarEntry entry : entries) {
                if (!entry.getName().startsWith("cli")) {
                    continue;
                }

                Path entryPath = Paths.get(entry.getName());
                if (entryPath.getNameCount() <= 1) {
                    continue;
                }

                String entryWithoutRootFolder = entryPath.subpath(1, entryPath.getNameCount()).toString();
                FilePath destination = tempDir.child(entryWithoutRootFolder);

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
            String defaultVersion = ResourceBundle.getBundle("config").getString(DEFAULT_CLI_VERSION_KEY);
            while (jellies.hasMoreElements()) {
                URL jellyU = jellies.nextElement();
                if (jellyU.getProtocol().equals("file")) {
                    File jellyF = new File(jellyU.toURI());
                    File classes = jellyF.getParentFile();
                    if (classes.getName().equals("classes")) {
                        FilePath wsClasses = new FilePath(classes).child("cli-" + defaultVersion);
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

    public static boolean isServerOSWindows() {
        return System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH).contains("win");
    }
}
