package com.uipath.uipathpackage.extensions;

import com.uipath.uipathpackage.actions.AddEnvironmentVariablesAction;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import jakarta.annotation.Nonnull;

@Extension
public class ExecutionEnvironmentVariablesContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run run, @Nonnull EnvVars envs, @Nonnull TaskListener listener) {
        AddEnvironmentVariablesAction envAction = run.getAction(AddEnvironmentVariablesAction.class);
        if (envAction != null) {
            envs.putAll(envAction.getAddedEnvironmentVariables());
        }
    }
}
