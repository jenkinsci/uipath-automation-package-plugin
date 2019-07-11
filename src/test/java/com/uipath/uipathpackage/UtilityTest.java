package com.uipath.uipathpackage;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class UtilityTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    TaskListener listener;
    @Mock
    PrintStream logger;
    @Mock
    EnvVars envVars;
    private FilePath tempDir;

    @Before
    public void beforeTest() throws IOException {
        File testDir = new File(System.getProperty("java.io.tmpdir"), "UiPathTest");
        if (!testDir.exists()) {
            if (!testDir.mkdir()) throw new IOException("Failed to create test directory");
        }
        FileUtils.cleanDirectory(testDir);
        tempDir = new FilePath(testDir);
    }

    @Test
    public void testImportModules() throws IOException, InterruptedException, URISyntaxException {
        when(listener.getLogger()).thenReturn(logger);
        doNothing().when(logger).println(isA(String.class));
        File resource = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("")).getPath());
        File jarFile = new File(resource, "uipath-automation-package.jar");
        when(envVars.expand(isA(String.class))).thenReturn(jarFile.getAbsolutePath());
        Utility util = spy(new Utility());
        Mockito.doReturn("1.0.6989.25854").when(util).getValue(isA(ResourceBundle.class), eq("UiPath.Extensions.Version"));
        Mockito.doReturn("19.4.0.17").when(util).getValue(isA(ResourceBundle.class), eq("UiPath.PowerShell.Version"));
        assertThat(util.importModuleCommands(tempDir, listener, envVars), CoreMatchers.containsString("UiPath.Extensions\\1.0.6989.25854\\RobotExecutor-PublicModule.psd1' -Force;Import-Module"));
        assertThat(util.importModuleCommands(tempDir, listener, envVars), CoreMatchers.containsString("UiPath.Extensions\\1.0.6989.25854\\UiPathPackage-Module.psd1' -Force;Import-Module"));
        assertThat(util.importModuleCommands(tempDir, listener, envVars), CoreMatchers.containsString("UiPath.PowerShell\\19.4.0.17\\UiPath.PowerShell.psd1' -Force"));
    }

    @Test(expected = InvalidParameterException.class)
    public void testValidateParams(){
        Utility util = new Utility();
        util.validateParams("", "testException");
    }
}
