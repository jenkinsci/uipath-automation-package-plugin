package com.uipath.uipathpackage.entries.job;

import com.uipath.uipathpackage.Messages;
import com.uipath.uipathpackage.entries.SelectEntry;
import com.uipath.uipathpackage.util.Utility;
import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class DynamicallyEntry extends SelectEntry {
    private final Integer jobsCount;
    private final String user;
    private final String machine;

    /**
     * Constructs jobsCount new instances of a job
     * @param jobsCount         The jobs count
     * @param user              The name of the user
     * @param machine           The name of the machine
     */
    @DataBoundConstructor
    public DynamicallyEntry(Integer jobsCount, String user, String machine) {
        this.jobsCount = jobsCount;
        this.user = user;
        this.machine = machine;
    }

    public Integer getJobsCount() { return jobsCount; }

    public String getUser() { return user; }

    public String getMachine() { return machine; }

    @Override
    public boolean validateParameters() {
        Utility util = new Utility();
        util.validateParams(jobsCount.toString(), "Invalid jobs count");
        return true;
    }

    @Symbol("Dynamically")
    @Extension
    public static class DescriptorImpl extends Descriptor<SelectEntry> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.DynamicallyEntry_DisplayName();
        }
    }
}
