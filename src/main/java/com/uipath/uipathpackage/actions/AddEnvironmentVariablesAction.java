package com.uipath.uipathpackage.actions;

import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import java.util.Map;

public class AddEnvironmentVariablesAction extends InvisibleAction implements EnvironmentContributingAction {
    private Map<String, String> addedEnvironmentVariables;

    public void setAddedEnvironmentVariables(Map<String, String> addedEnvironmentVariables) {
        this.addedEnvironmentVariables = addedEnvironmentVariables;
    }

    public Map<String, String> getAddedEnvironmentVariables() {
        return addedEnvironmentVariables;
    }

    public boolean HasEnvironmentVariablesToAdd() {
        return addedEnvironmentVariables != null && !addedEnvironmentVariables.isEmpty();
    }
}