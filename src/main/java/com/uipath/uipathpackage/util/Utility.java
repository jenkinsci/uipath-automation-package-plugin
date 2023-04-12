package com.uipath.uipathpackage.util;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
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

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static com.uipath.uipathpackage.util.TraceLevel.Information;
import static com.uipath.uipathpackage.util.TraceLevel.Verbose;

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
    public void validateParams(@Nonnull String param,
                               @Nonnull String s) {
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
    public String getConfigValue(@Nonnull ResourceBundle rb,
                                 @Nonnull String s) {
        return rb.getString(s);
    }

    /**
     * A common logger for all the tasks
     * @param traceLevel
     * @param listener
     * @param logMessage
     */
    public void logger(TraceLevel traceLevel,
                       TaskListener listener,
                       String logMessage) {

        if (Objects.isNull(traceLevel)
                || Objects.isNull(listener)
                || Strings.isNullOrEmpty(logMessage)) {
            return;
        }
        switch (traceLevel) {
            case None:
            case Verbose:
            case Information:
                listener.getLogger().println(logMessage);
                break;
            case Error:
                listener.error(logMessage);
                break;
            case Warning:
            case Critical:
            default:
        }
    }

    public EnvVars setWorkspaceEnvVariableInCaseNotPresent(FilePath workspace,
                                                           TaskListener listener,
                                                           EnvVars envVars) {
        /**
         * Tweaking the WORKSPACE environment variable for the multibranch pipeline
         * as it is not present for that type of project, hence we will use workspace
         * from the arguments and set it as the value of WORKSPACE environment variable
         */
        if (!envVars.containsKey("WORKSPACE")) {
            logger(Verbose, listener, "Setting up the ${WORKSPACE} environment variable with value "
                    + workspace.getRemote().toString() + " as it is not found in the current context");
            envVars.put("WORKSPACE", workspace.getRemote().toString());
        }
        return envVars;
    }

    public int execute(@Nonnull String command,
                       @Nonnull SerializableCliOptions options,
                       @Nonnull FilePath remoteTempDir,
                       @Nonnull TaskListener listener,
                       @Nonnull EnvVars envVars,
                       @Nonnull Launcher launcher,
                       boolean throwExceptionOnFailure) throws IOException,
                                                               InterruptedException,
                                                               URISyntaxException {
        if (remoteTempDir.getRemote().toUpperCase().contains(":\\WINDOWS\\SYSTEM32")) {
            throw new AbortException("The plugin cannot be executed in a workspace path inside the WINDOWS folder. Please use a custom workspace folder that is outside the WINDOWS folder for this build definition or reinstall Jenkins and use a local user account instead.");
        }

        UiPathCliConfiguration cliConfiguration = UiPathCliConfiguration.getInstance();
        Optional<FilePath> cachedCliPath = cliConfiguration.getCliPath(launcher, envVars, cliConfiguration.getSelectedOrDefaultCliVersionKey());

        FilePath cliPath;
        if (cachedCliPath.isPresent()) {
            cliPath = cachedCliPath.get();
            logger(Verbose, listener, "Using the cached CLI path " + cliPath.getRemote().toString());
        } else {
            FilePath cliRootCacheDirPath = cliConfiguration.getCliRootCachedDirectoryPath(launcher, envVars, cliConfiguration.getDefaultCliVersionKey());
            cliPath = extractCliApp(cliRootCacheDirPath, listener, envVars);
            logger(Verbose, listener, "Using the explicitly selected CLI path " + cliPath.getRemote().toString());
        }

        FilePath commandOptionsFile = remoteTempDir.createTextTempFile("uipcliargs", "", new JSONObject(new RunOptions(command, options)).toString());

        int result = launcher
                .launch()
                .cmds(this.buildCommandLine(cliPath, commandOptionsFile, listener))
                .envs(envVars)
                .stdout(listener)
                .pwd(cliPath.getParent())
                .start()
                .join();

        if (throwExceptionOnFailure && result != 0) {
            throw new AbortException("Failed to run the command, the CLI failed with error code " + result);
        }

        return result;
    }

    public void validateRuntime(@Nonnull Launcher launcher) throws AbortException,
                                                                   JsonProcessingException {
        UiPathCliConfiguration configuration = UiPathCliConfiguration.getInstance();
        String selectedCliVersionKey = configuration.getSelectedOrDefaultCliVersionKey();
        UiPathCliConfiguration.Configuration cliConfig = configuration.getConfiguration().get(selectedCliVersionKey);

        if (launcher.isUnix() && cliConfig.getWindowsCompatible()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseLinux());
        }
        if (cliConfig.getLinuxCompatible() && !launcher.isUnix()) {
            throw new AbortException(com.uipath.uipathpackage.Messages.GenericErrors_MustUseWindows());
        }
    }

    /**
     * This method is only to be used for extracting legacy cli (ver.21.xx.xxx.xxx).
     * This method needs to be changed once we deprecate the legacy cli and start packing
     * a different version of cli i.e. greater than or equal 22.xx.xxx.xxx as line 129 of
     * this method tries to locate uipcli.exe after extracting the cli's nuget, if used
     * for other versions will result in runtime failures.
     */
    public FilePath extractCliApp(@Nonnull FilePath targetRootCacheDir,
                                  @Nonnull TaskListener listener,
                                  @Nonnull EnvVars env) throws IOException,
                                                               InterruptedException,
                                                               URISyntaxException {
        PrintStream logger = listener.getLogger();
        ResourceBundle rb = ResourceBundle.getBundle("config");
        String cliFolderName = UiPathCliConfiguration.LEGACY_CLI_PREFIX + this.getConfigValue(rb, "UiPath.CLI.Version");
        FilePath targetCliPath = targetRootCacheDir.child(cliFolderName).child("lib").child("net461").child("uipcli.exe");
        if (targetCliPath.exists()) {
            logger(Information, listener, "Using previously extracted UiPath CLI from " + targetCliPath);
            return targetCliPath;
        }

        String pluginJarPath;

        if (isServerOSWindows()) {
            pluginJarPath = env.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar");
        } else {
            pluginJarPath = env.expand("${JENKINS_HOME}/plugins/uipath-automation-package/WEB-INF/lib/uipath-automation-package.jar");
        }

        logger(Information, listener, "Expected plugin jar path on Jenkins master: " + pluginJarPath + ", extracting...");

        // Copy relevant files to temp directory
        copyPluginFilesToTempDir(listener, targetRootCacheDir, pluginJarPath);
        return targetCliPath;
    }

    public void downloadCli(String feedUrl,
                            @Nonnull FilePath downloadPath,
                            @Nonnull TaskListener listener) throws AbortException {
        PrintStream logger = listener.getLogger();

        RestTemplate restTemplate = new RestTemplate();
        try {
            logger(Information, listener, "Downloading CLI from "+ feedUrl);

            RequestCallback requestCallback = request -> request.getHeaders()
                    .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

            ResponseExtractor<Void> responseExtractor = response -> {
                try {
                    downloadPath.copyFrom(response.getBody());
                } catch (InterruptedException e) {
                    e.printStackTrace(logger);
                    throw new AbortException("error while writing nupkg to download directory ");
                }
                return null;
            };

            restTemplate.execute(feedUrl, HttpMethod.GET, requestCallback, responseExtractor);

            logger(Information, listener, "Downloaded CLI successfully. @"+ downloadPath);
        }
        catch (HttpClientErrorException hcre) {
            if (Arrays.asList(301,302,303).contains(hcre.getRawStatusCode())) {
                logger(Information, listener, "Retrying Downloading CLI....");
                downloadCli(hcre.getResponseHeaders().getLocation().toString(),downloadPath, listener);
            }
            logger(Information, listener,"Aborting Task Unable to Download CLI.... HttpStatus " + hcre.getRawStatusCode()+ " Response " + hcre.getResponseBodyAsString() + " Error " + hcre.getMessage());
            throw new AbortException("unable to download the CLI from the public feed");
        }
        catch (RestClientException rce) {
            logger(Information, listener, "Aborting Task Unable to Download CLI.... Error "+ rce.getMessage() + " Download Path "+downloadPath.getRemote());
            throw new AbortException("unable to download the CLI from the public feed");
        }
    }

    public void setCredentialsFromCredentialsEntry(SelectEntry credentials,
                                                   AuthenticatedOptions options,
                                                   @Nonnull Run<?, ?> run) throws AbortException {
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
            options.setApplicationScope(cred.getApplicationScope());
            if (StringUtils.isNotBlank(cred.getIdentityUrl())) {
            	options.setAuthorizationUrl(cred.getIdentityUrl());
            } else {
            	options.setAuthorizationUrl(options.getOrchestratorUrl());
            }
        }
    }

    public void setJobRunFromStrategyEntry(SelectEntry strategy,
                                           JobOptions options) {
        if (Objects.isNull(strategy)) {
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
        } else {
            String robotNames = ((RobotEntry) strategy).getRobotsIds();
            if (robotNames != null) {
                options.setRobots(robotNames.split(","));
            } else {
                options.setRobots(new String[]{});
            }
        }
    }

    public void setJobRunFromJobTypeEntry(SelectEntry strategy,
                                          JobOptions options) {
        if (strategy instanceof NonProductionJobTypeEntry) {
            options.setJobType(JobType.NonProduction);
        }
        else if (strategy instanceof UnattendedJobTypeEntry) {
            options.setJobType(JobType.Unattended);
        } else if (strategy instanceof TestAutomationJobTypeEntry) {
        	options.setJobType(JobType.TestAutomation);
        }
    }

    private String[] buildCommandLine(FilePath cliPath,
                                      FilePath commandOptionsFile,
                                      TaskListener listener) throws JsonProcessingException {
        PrintStream logger = listener.getLogger();
        UiPathCliConfiguration configuration = UiPathCliConfiguration.getInstance();
        String selectedCliVersionKey = System.getProperty(UiPathCliConfiguration.SELECTED_CLI_VERSION_KEY);


        if (StringUtils.isBlank(selectedCliVersionKey)) {
            selectedCliVersionKey = configuration.getDefaultCliVersionKey();
            logger(Verbose, listener, "None of the CLI is explicitly selected (through install platform task), falling back to default CLI version: " + selectedCliVersionKey);
        }
        logger(Verbose, listener, "Selected CLI version is " + selectedCliVersionKey);

        UiPathCliConfiguration.Configuration cliConfig = configuration.getConfiguration().get(selectedCliVersionKey);

        String[] finalCommand = new String[] { cliPath.getRemote(), "run", commandOptionsFile.getRemote() };
        if (cliConfig.getVersion().getMajor() >= 22) {
            finalCommand = new String[] {"dotnet", cliPath.getRemote(), "run", commandOptionsFile.getRemote()};
        }
        return finalCommand;
    }

    private void extractResourcesToTempFolder(FilePath tempDir,
                                              File jarfile,
                                              TaskListener listener) throws IOException,
                                                                            InterruptedException {
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

    private void copyPluginFilesToTempDir(@Nonnull TaskListener listener,
                                          FilePath tempDir,
                                          String pluginJarPath) throws IOException,
                                                                       URISyntaxException,
                                                                       InterruptedException {
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
                        logger(Information, listener, "Plugin files copied to temp");
                    }
                }
            }
        } else {
            logger(Information, listener, "Extracting cli to temp folder " + tempDir.getRemote() + "...");
            extractResourcesToTempFolder(tempDir, jar, listener);
            logger(Information, listener, "done!" );
        }
    }

    public static boolean isServerOSWindows() {
        return System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH).contains("win");
    }


}
