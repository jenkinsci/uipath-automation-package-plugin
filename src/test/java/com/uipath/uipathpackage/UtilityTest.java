package com.uipath.uipathpackage;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class UtilityTest {

    @Mock
    TaskListener listener;

    @Mock
    PrintStream logger;

    @Mock
    EnvVars envVars;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testImportModules() throws IOException, PowerShellExecutionException {
        when(listener.getLogger()).thenReturn(logger);
        doNothing().when(logger).println(isA(String.class));
        File resource = new File(getClass().getClassLoader().getResource("").getPath());
        File jarFile = new File(resource, "uipath-automation-package.jar");
        when(envVars.expand(isA(String.class))).thenReturn(jarFile.getAbsolutePath());
        try (PowerShell powerShell = PowerShell.open()) {
            Utility util = spy(new Utility());
            Mockito.doReturn("1.0.6989.25854").when(util).getValue(isA(ResourceBundle.class), eq("UiPath.Extensions.Version"));
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File tempDir = new File(baseDir, "UiPath");
            assertEquals(tempDir, util.importModules(listener, powerShell, envVars));
        }
    }
}
