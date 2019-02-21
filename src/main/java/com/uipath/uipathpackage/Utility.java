package com.uipath.uipathpackage;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Utility Class used by UiPathDeploy and UiPathPack
 */
class Utility {

    /*
    method to create a working directory and import static powershell modules for pack
     */
    File importModules(@Nonnull TaskListener listener, PowerShell powerShell, EnvVars env) throws PowerShellExecutionException, IOException {
        listener.getLogger().println("Importing Powershell and extensions modules");
        File tempDir= getTempDir();
        String response = powerShell.executeCommands("cd \""+StringEscapeUtils.escapeJava(tempDir.getAbsolutePath())+"\"");
        validateExecutionStatus(powerShell, response, "Error while changing to temp directory: ");
        listener.getLogger().println(response);
        String pluginJarPath = env.expand("${JENKINS_HOME}\\plugins\\uipath-automation-package\\WEB-INF\\lib\\uipath-automation-package.jar");
        listener.getLogger().println("plugin jar path is : "+pluginJarPath);
        //Copy relevant files to temp directory
        copyPluginFiles(listener, powerShell, tempDir, pluginJarPath);
        //import robot executor, UiPath package module
        ResourceBundle rb = ResourceBundle.getBundle("config");
        response = powerShell.executeCommands("Import-Module \"" + StringEscapeUtils.escapeJava(new File(tempDir,"UiPath.Extensions/"+rb.getString("UiPath.Extensions.Version")+"/RobotExecutor-PublicModule.psd1").getAbsolutePath()) + "\" -Force");
        validateExecutionStatus(powerShell, response, "Error while importing module RobotExecutor. :");
        response = powerShell.executeCommands("Import-Module \"" + StringEscapeUtils.escapeJava(new File(tempDir,"UiPath.Extensions/"+rb.getString("UiPath.Extensions.Version")+"/UiPathPackage-Module.psd1").getAbsolutePath()) + "\" -Force");
        validateExecutionStatus(powerShell, response, "Error while importing module UiPathPackage :");
        listener.getLogger().println("Module imported");
        return tempDir;
    }

    void validateExecutionStatus(PowerShell powerShell, String response, String s) throws PowerShellExecutionException, IOException {
        if ("0".equals(powerShell.executeCommands("$LASTEXITCODE"))) {
            throw new RuntimeException(s + response);
        }
    }

    /**
     * Generates the package
     * @param packagePath Package path
     * @param outputPath Output Path
     * @param powerShell Powershell to execute commands
     * @param version Project Version
     * @return String package result
     * @throws PowerShellExecutionException returned while executing commands
     * @throws IOException returned while executing commands
     */
    String generatePackage(String packagePath, String outputPath, PowerShell powerShell, String version) throws PowerShellExecutionException, IOException {
        String response;
        if(version == null)
            response = powerShell.executeCommands("Pack -projectJsonPath \""+packagePath+"\" -outputFolder \""+outputPath+"\"");
        else
            response = powerShell.executeCommands("Pack -projectJsonPath \""+ packagePath+"\" -packageVersion \""+version+"\" -outputFolder \""+ outputPath+"\"");
        validateExecutionStatus(powerShell, response, "Error while Packaging the project: ");
        return response;
    }

    /**
     * Deploys the package to orchestrator address
     * @param orchestratorAddress Orchestrator Base URL
     * @param packagePath   Package Path
     * @param orchestratorTenantFormatted Orchestrator Tenant
     * @param username Orchestrator Username
     * @param password Orchestrator Password
     * @param powerShell powershell to execute commands
     * @return String deploy result
     * @throws PowerShellExecutionException returned while executing commands
     * @throws IOException returned while executing commands
     */
    String deployPackage(String orchestratorAddress, String packagePath, String orchestratorTenantFormatted, String username, String password, PowerShell powerShell) throws PowerShellExecutionException, IOException {
        String response = powerShell.executeCommands("Deploy -orchestratorAddress \""+orchestratorAddress+"\" -tenant \""+orchestratorTenantFormatted+"\" -username \""+username+"\" -password \""+password+"\" -packagePath \""+packagePath+"\" -authType UserPass");
        validateExecutionStatus(powerShell, response, "Error while deploying the project: ");
        return response;
    }

    /**
     * Validates the param for null or empty check
     * @param param Param to validate
     * @param s Error Message
     */
    void validateParams(String param, String s) {
        if (param == null || param.trim().isEmpty())
            throw new RuntimeException(s);
    }

    private void copyPluginFiles(@Nonnull TaskListener listener, PowerShell powerShell, File tempDir, String pluginJarPath) throws IOException, PowerShellExecutionException {
        String response;
        File jar = new File(pluginJarPath);
        if(!jar.exists()){
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
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    File classes = jellyF.getParentFile();
                    if (classes.getName().equals("classes")) {
                        response = powerShell.executeCommands("Copy-Item -Path \"" + StringEscapeUtils.escapeJava(classes.getAbsolutePath()) + "\\*\" -Destination \"" + StringEscapeUtils.escapeJava(tempDir.getAbsolutePath()) + "\" -Recurse -force");
                        validateExecutionStatus(powerShell, response, "Error while copying project to temp: ");
                        listener.getLogger().println("Files copied to temp "+response);
                    }
                }
            }
        }
        else {
            response = powerShell.executeCommands("Copy-Item -Path \"" + StringEscapeUtils.escapeJava(pluginJarPath) + "\" -Destination \"" + StringEscapeUtils.escapeJava(tempDir.getAbsolutePath()) + "\" -force");
            validateExecutionStatus(powerShell, response, "Error while copying jar to temp: ");
            listener.getLogger().println(response);
            String jarPath = new File(tempDir, "uipath-automation-package.jar").getAbsolutePath();
            response = powerShell.executeCommands("jar -xf \"" + StringEscapeUtils.escapeJava(jarPath) + "\"");
            validateExecutionStatus(powerShell, response, "Error while extracting the contents of jar to temp: ");
            listener.getLogger().println("jar copied to temp and extracted "+response);
        }
    }

    private File getTempDir() throws IOException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(baseDir,"UiPath");
        if(!tempDir.exists()) {
            boolean result = tempDir.mkdir();
            if(!result){
                throw new RuntimeException("Failed to create temp directory");
            }
        }
        FileUtils.cleanDirectory(tempDir);
        return tempDir;
    }
}
