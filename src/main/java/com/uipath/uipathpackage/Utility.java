package com.uipath.uipathpackage;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Utility Class used by UiPathDeploy and UiPathPack
 */
class Utility {

    /**
     * Wraps a string in quotes and escapes all PowerShell special characters.
     * This is a helper method for creating strings that will be interpreted
     * literally by PowerShell.
     *
     * @param s the string to be escaped.
     * @return an escaped string.
     */
    static String escapePowerShellString(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    /***
     * method to create a working directory and get command for static powershell modules for pack
     * @param listener Task Listener to log the results
     * @param env Has EnvVars
     * @return commands which needed to be executed to import modules
     * @throws IOException when failed to read input or write output in powershell
     */
    String importModuleCommands(@Nonnull TaskListener listener, EnvVars env) throws IOException, URISyntaxException {
        File tempDir = getTempDir();
        String pluginJarPath = env.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar");
        listener.getLogger().println("plugin jar path is : " + pluginJarPath);
        //Copy relevant files to temp directory
        copyPluginFiles(listener, tempDir, pluginJarPath);
        //import robot executor, UiPath package module commands
        ResourceBundle rb = ResourceBundle.getBundle("config");
        String robotExecutorModule = "Import-Module " + escapePowerShellString(new File(tempDir, "UiPath.Extensions/" + getValue(rb, "UiPath.Extensions.Version") + "/RobotExecutor-PublicModule.psd1").getAbsolutePath()) + " -Force";
        String uipathPackageModule = "Import-Module " + escapePowerShellString(new File(tempDir, "UiPath.Extensions/" + getValue(rb, "UiPath.Extensions.Version") + "/UiPathPackage-Module.psd1").getAbsolutePath()) + " -Force";
        String uipathPowershellModule = "Import-Module " + escapePowerShellString(new File(tempDir, "UiPath.PowerShell/" + getValue(rb, "UiPath.PowerShell.Version") + "/UiPath.PowerShell.psd1").getAbsolutePath()) + " -Force";
        return getCommand(robotExecutorModule,uipathPackageModule,uipathPowershellModule);
    }

    /**
     * Executes the list of powershell commands
     *
     * @param listener TaskListener to log the results to console output
     * @param commands powershell commands in string
     * @throws IOException in case it fails to read/write in buffered stream while executing command
     */
    void execute(@Nonnull TaskListener listener, String... commands) throws IOException {
        boolean successStatus = true;
        String commandChain = getCommand(commands);
        String wrappedCommandChain = "powershell.exe -ExecutionPolicy Bypass Invoke-Expression " + escapePowerShellString(commandChain);
        // Executing the command
        Process powerShellProcess = Runtime.getRuntime().exec(wrappedCommandChain);
        // Getting the results
        powerShellProcess.getOutputStream().close();
        String line;
        listener.getLogger().println("Standard Output:");
        BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream(), StandardCharsets.UTF_8));
        while ((line = stdout.readLine()) != null) {
            listener.getLogger().println(line);
        }
        stdout.close();
        BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder error = new StringBuilder();
        while ((line = stderr.readLine()) != null) {
            successStatus = false;
            error.append(line);
        }
        stderr.close();
        listener.getLogger().println("Done");
        if (!successStatus) {
            throw new AbortException("Error while executing powershell commands for importing modules and pack/deploy\n" + error);
        }
    }

    /**
     * Validates the param for null or empty check
     *
     * @param param Param to validate
     * @param s     Error Message
     */
    void validateParams(String param, String s) {
        if (param == null || param.trim().isEmpty()){
            throw new InvalidParameterException(s);
        }
    }

    private String getCommand(String... commands) {
        StringBuilder commandChainBuilder = new StringBuilder();
        for (String command : commands) {
            commandChainBuilder.append(command);
            commandChainBuilder.append(";");
        }
        return commandChainBuilder.toString();
    }

    private void copyPluginFiles(@Nonnull TaskListener listener, File tempDir, String pluginJarPath) throws IOException, URISyntaxException {
        File jar = new File(pluginJarPath);
        if (!jar.exists()) {
            // For snapshot plugin dependencies, an IDE may have replaced ~/.m2/repository/…/${artifactId}.hpi with …/${artifactId}-plugin/target/classes/
            // which unfortunately lacks META-INF/MANIFEST.MF so try to find index.jelly (which every plugin should include) and thus the ${artifactId}.hpi:
            Enumeration<URL> jellies = getClass().getClassLoader().getResources("index.jelly");
            while (jellies.hasMoreElements()) {
                URL jellyU = jellies.nextElement();
                if (jellyU.getProtocol().equals("file")) {
                    File jellyF;
                    try {
                        jellyF = new File(jellyU.toURI());
                    } catch (URISyntaxException e) {
                        e.printStackTrace(listener.getLogger());
                        throw e;
                    }
                    File classes = jellyF.getParentFile();
                    if (classes.getName().equals("classes")) {
                        execute(listener, "Copy-Item -Path " + escapePowerShellString(classes.getAbsolutePath() + "\\*") + " -Destination " + escapePowerShellString(tempDir.getAbsolutePath()) + " -Recurse -force");
                        listener.getLogger().println("Files copied to temp");
                    }
                }
            }
        } else {
            listener.getLogger().println("extracting powershell modules to temp folder");
            extractResourcesToTempFolder(tempDir, jar, listener);
            listener.getLogger().println("extracted powershell modules to temp folder");
        }
    }

    private File getTempDir() throws IOException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(baseDir, "UiPath");
        if (!tempDir.exists()) {
            boolean result = tempDir.mkdir();
            if (!result) {
                throw new AbortException("Failed to create temp directory");
            }
        }
        FileUtils.cleanDirectory(tempDir);
        return tempDir;
    }

    private void extractResourcesToTempFolder(File tempDir, File jarfile, TaskListener listener) throws IOException {
        try (JarFile archive = new JarFile(jarfile)) {
            // sort entries by name to always create folders first
            List<? extends JarEntry> entries = archive.stream().sorted(Comparator.comparing(JarEntry::getName)).collect(Collectors.toList());
            for (JarEntry entry : entries) {
                ResourceBundle rb = ResourceBundle.getBundle("config");
                if (!entry.getName().startsWith(getValue(rb, "UiPath.PowerShell.Name")) && !entry.getName().startsWith(getValue(rb, "UiPath.Extensions.Name"))) {
                    continue;
                }
                Path entryDest = tempDir.toPath().resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectory(entryDest);
                } else {
                    Files.copy(archive.getInputStream(entry), entryDest);
                }
            }
        } catch (JarException e) {
            e.printStackTrace(listener.getLogger());
            throw e;
        }
    }

    String getValue(ResourceBundle rb, String s) {
        return rb.getString(s);
    }

}
