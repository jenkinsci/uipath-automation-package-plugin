package com.uipath.uipathpackage.entries;

import hudson.AbortException;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;

/**
 * Partial default implementation of {@link Describable}.
 */
public abstract class SelectEntry extends AbstractDescribableImpl<SelectEntry> {
    public abstract boolean validateParameters() throws AbortException;
}
