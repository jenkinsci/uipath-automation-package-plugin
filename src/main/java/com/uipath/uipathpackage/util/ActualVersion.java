package com.uipath.uipathpackage.util;

public class ActualVersion {
    private final int major;
    private final int minor;

    public ActualVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public boolean supportsNewTelemetry() {
        return major >= 23 && minor >= 10;
    }
}