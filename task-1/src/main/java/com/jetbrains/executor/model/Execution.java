package com.jetbrains.executor.model;

import java.time.Instant;

public class Execution {

    public enum Status { QUEUED, IN_PROGRESS, FINISHED, FAILED }

    private final String id;
    private final String script;
    private final ResourceSpec resources;
    private volatile Status status;
    private volatile String output;
    private volatile String error;
    private final Instant createdAt;

    public Execution(String id, String script, ResourceSpec resources) {
        this.id = id;
        this.script = script;
        this.resources = resources;
        this.status = Status.QUEUED;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getScript() { return script; }
    public ResourceSpec getResources() { return resources; }
    public Status getStatus() { return status; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(Status status) { this.status = status; }
    public void setOutput(String output) { this.output = output; }
    public void setError(String error) { this.error = error; }
}
