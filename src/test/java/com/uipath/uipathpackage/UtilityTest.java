package com.uipath.uipathpackage;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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
            Utility util = new Utility();
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File tempDir = new File(baseDir, "UiPath");
            assertEquals(tempDir, util.importModules(listener, powerShell, envVars));
        }
    }
}
