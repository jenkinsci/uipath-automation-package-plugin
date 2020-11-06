package com.uipath.uipathpackage.entries.job;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class RobotEntry extends SelectEntry {
    private final String robotsIds;

    /**
     * Constructs a new instance of a robot entry job
     * @param robotsIds The robots ids
     */
    @DataBoundConstructor
    public RobotEntry(String robotsIds) { this.robotsIds = robotsIds; }

    /**
     * Gets the robots ids
     * @return String robotsIds
     */
    public String getRobotsIds() {
        return robotsIds;
    }

    @Override
    public boolean validateParameters() throws AbortException {
        Utility util = new Utility();
        util.validateParams(robotsIds, "Invalid list of robots ids");
        return true;
    }

    @Symbol("Robot")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.RobotEntry_DisplayName();
        }
    }
}
