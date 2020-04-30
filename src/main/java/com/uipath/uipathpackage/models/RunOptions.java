package com.uipath.uipathpackage.models;

public class RunOptions {
    private String type;
    private SerializableCliOptions options;

    public RunOptions(String type, SerializableCliOptions options) {
        this.type = type;
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SerializableCliOptions getOptions() {
        return options;
    }

    public void setOptions(SerializableCliOptions options) {
        this.options = options;
    }
}
