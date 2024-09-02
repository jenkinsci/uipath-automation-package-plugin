package com.uipath.uipathpackage.actions;

import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import java.util.Map;

public class AddEnvironmentVariablesAction extends InvisibleAction implements EnvironmentContributingAction {
    private final Map<String, String> addedEnvironmentVariables;

    public AddEnvironmentVariablesAction(Map<String, String> addedEnvironmentVariables) {
        this.addedEnvironmentVariables = addedEnvironmentVariables;
    }

    public Map<String, String> getAddedEnvironmentVariables() {
        return addedEnvironmentVariables;
    }
}
