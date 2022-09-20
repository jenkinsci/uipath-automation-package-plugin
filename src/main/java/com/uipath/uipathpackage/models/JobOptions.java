package com.uipath.uipathpackage.models;

import com.uipath.uipathpackage.util.JobType;
import com.uipath.uipathpackage.util.StartProcessDtoJobPriority;

public class JobOptions extends AuthenticatedOptions {
    private String processName;
    private String parametersFilePath;
    private static final String telemetryOrigin = "Jenkins";
    private StartProcessDtoJobPriority priority = StartProcessDtoJobPriority.Normal;

    private Integer jobsCount = 1;
    private String[] robots;

    private String user;
    private String machine;

    private String resultFilePath;
    private Integer timeout = 1800;
    private boolean failWhenJobFails = true;
    private boolean waitForJobCompletion = true;
    private JobType jobType = null;

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getTelemetryOrigin() {
        return telemetryOrigin;
    }

    public String getParametersFilePath() {
        return parametersFilePath;
    }

    public void setParametersFilePath(String parametersFilePath) {
        if (parametersFilePath!= null && !parametersFilePath.isEmpty()) {
            this.parametersFilePath = parametersFilePath;
        }
    }

    public Integer getJobsCount() { return jobsCount; }

    public void setJobsCount(Integer jobsCount) {
        if (jobsCount != null) {
            this.jobsCount = jobsCount;
        }
    }

    public String getUser() { return user; }

    public void setUser(String user) { this.user = user; }

    public String getMachine() { return machine; }

    public void setMachine(String machine) { this.machine = machine; }

    public String getResultFilePath() { return resultFilePath; }

    public void setResultFilePath(String resultFilePath) {
        if (resultFilePath != null && !resultFilePath.isEmpty()) {
            this.resultFilePath = resultFilePath;
        }
    }

    public StartProcessDtoJobPriority getPriority() { return priority; }

    public void setPriority(StartProcessDtoJobPriority priority) {
        if (priority != null) {
            this.priority = priority;
        }
    }

    public String[] getRobots() {
        if (robots != null)
            return robots.clone();

        return null;
    }

    public void setRobots(String[] robots) {
        if (robots != null) {
            this.robots = robots.clone();
        } else {
            this.robots = null;
        }
    }

    public Integer getTimeout() { return timeout; }

    public void setTimeout(Integer timeout) {
        if (timeout != null) {
            this.timeout = timeout;
        }
    }

    public boolean isFailWhenJobFails() {
        return failWhenJobFails;
    }

    public void setFailWhenJobFails(boolean failWhenJobFails) {
        this.failWhenJobFails = failWhenJobFails;
    }

    public boolean isWaitForJobCompletion() {
        return waitForJobCompletion;
    }

    public void setWaitForJobCompletion(boolean waitForJobCompletion) {
        this.waitForJobCompletion = waitForJobCompletion;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
}
