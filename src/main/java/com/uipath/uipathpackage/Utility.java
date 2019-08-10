package com.uipath.uipathpackage;

import hudson.*;
import hudson.model.TaskListener;
import hudson.tasks.Messages;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

    protected String command;

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
     * method to create a working directory and get command for static powershell modules for pack
     * @param listener Task Listener to log the results
     * @param env Has EnvVars
     * @return commands which needed to be executed to import modules
     * @throws IOException when failed to read input or write output in powershell
     */
    public String importModuleCommands(@Nonnull FilePath tempDir, @Nonnull TaskListener listener, @Nonnull EnvVars env) throws IOException, URISyntaxException, InterruptedException {
        String pluginJarPath;
        if (isCurrentOSWindows()) {
            pluginJarPath = env.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar");
        } else {
            pluginJarPath = env.expand("${JENKINS_HOME}/plugins/uipath-automation-package/WEB-INF/lib/uipath-automation-package.jar");
        }
        listener.getLogger().println("expected plugin jar path is : " + pluginJarPath);
        ResourceBundle rb = ResourceBundle.getBundle("config");
        //Copy relevant files to temp directory
        copyPluginFiles(listener, tempDir, pluginJarPath, rb);
        String importModuleTemplate = "Import-Module %s -Force";
        String robotExecutorModule = String.format(importModuleTemplate, escapePowerShellString(tempDir.child("UiPath.Extensions/" + this.getValue(rb, "UiPath.Extensions.Version") + "/RobotExecutor-PublicModule.psd1").getRemote()));
        String uipathPackageModule = String.format(importModuleTemplate, escapePowerShellString(tempDir.child("UiPath.Extensions/" + this.getValue(rb, "UiPath.Extensions.Version") + "/UiPathPackage-Module.psd1").getRemote()));
        String uipathPowershellModule = String.format(importModuleTemplate, escapePowerShellString(tempDir.child("UiPath.PowerShell/" + this.getValue(rb, "UiPath.PowerShell.Version") + "/UiPath.PowerShell.psd1").getRemote()));
        return getCommand(robotExecutorModule, uipathPackageModule, uipathPowershellModule);
    }

    private boolean isCurrentOSWindows() {
        return System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH).contains("win");
    }

    private void copyPluginFiles(@Nonnull TaskListener listener, FilePath tempDir, String pluginJarPath, ResourceBundle rb) throws IOException, URISyntaxException, InterruptedException {
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
                        listener.getLogger().println("Files copied to temp");
                    }
                }
            }
        } else {
            listener.getLogger().println("extracting powershell modules to temp folder");
            extractResourcesToTempFolder(tempDir, jar, listener, rb);
            listener.getLogger().println("extracted powershell modules to temp folder");
        }
    }

    /**
     * Wraps a string in quotes and escapes all PowerShell special characters.
     * This is a helper method for creating strings that will be interpreted
     * literally by PowerShell.
     *
     * @param s the string to be escaped.
     * @return an escaped string.
     */
    public String escapePowerShellString(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    /***
     * Returns the bundle value of config
     * @param rb ResourceBundle
     * @param s key of the bundle
     * @return value of the resource bundle corresponding to key
     */
    public String getValue(@Nonnull ResourceBundle rb, @Nonnull String s) {
        return rb.getString(s);
    }

    /***
     * Merge commands to return single command
     * @param commands Sequence of command
     * @return Chained single command
     */
    public String getCommand(@Nonnull String... commands) {
        StringBuilder commandChainBuilder = new StringBuilder();
        for (String command : commands) {
            commandChainBuilder.append(command);
            commandChainBuilder.append(";");
        }
        return commandChainBuilder.toString();
    }

    public boolean execute(FilePath ws, TaskListener listener, EnvVars envVars, Launcher launcher) throws IOException, InterruptedException{
        FilePath script = null;
        try {
            script = this.createScriptFile(ws);
            int r = -1;
            r = launcher.launch().cmds(this.buildCommandLine(script)).envs(envVars).stdout(listener).pwd(ws).start().join();
            return r == 0;
        } finally {
            try {
                if (script != null) {
                    script.delete();
                }
            } catch (Exception var22) {
                Functions.printStackTrace(var22, listener.fatalError(Messages.CommandInterpreter_UnableToDelete(script)));
            }
        }
    }

    private String[] buildCommandLine(FilePath script) {
        return new String[] { "powershell.exe", "-NonInteractive", "-ExecutionPolicy", "ByPass", "& \'" + script.getRemote() + "\'"};
    }

    public FilePath createScriptFile(@Nonnull FilePath dir) throws IOException, InterruptedException {
        return dir.createTextTempFile("jenkins", ".ps1", this.getContents(), false);
    }

    protected String getContents() {
        return command + "\r\nexit $LastExitCode";
    }

    private void extractResourcesToTempFolder(FilePath targetDir, File jarfile, TaskListener listener, ResourceBundle rb) throws IOException, InterruptedException {
        try (JarFile archive = new JarFile(jarfile)) {
            // sort entries by name to always create folders first
            List<? extends JarEntry> entries = archive.stream().sorted(Comparator.comparing(JarEntry::getName)).collect(Collectors.toList());
            File tempDir = getTempDir();
            for (JarEntry entry : entries) {
                if (!entry.getName().startsWith(getValue(rb, "UiPath.PowerShell.Name")) && !entry.getName().startsWith(getValue(rb, "UiPath.Extensions.Name"))) {
                    continue;
                }
                Path entryDest = tempDir.toPath().resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectory(entryDest);
                } else {
                    Files.copy(archive.getInputStream(entry), entryDest);
                }
                new FilePath(tempDir).copyRecursiveTo(targetDir);
            }
        } catch (JarException e) {
            e.printStackTrace(listener.getLogger());
            throw e;
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
}
