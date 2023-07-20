package com.uipath.uipathpackage.util;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

public class TaskScopedEnvVarsManager {
    public static EnvVars selectOnlyRequiredEnvironmentVariables(@Nonnull Run<?, ?> run, @Nonnull EnvVars env, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        EnvVars envVars = run.getEnvironment(listener);
        envVars.put("WORKSPACE", env.get("WORKSPACE"));
        if (env.get("UIPATH_EXTENSIONS_CLI_TELEMETRY_ENABLED") != null)
            envVars.put("UIPATH_EXTENSIONS_CLI_TELEMETRY_ENABLED", env.get("UIPATH_EXTENSIONS_CLI_TELEMETRY_ENABLED"));

        return envVars;
    }
}
