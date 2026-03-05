package com.jetbrains.executor.model;

public class ExecutionRequest {

    private String script;
    private ResourceSpec resources;

    public ExecutionRequest() {}

    public String getScript() { return script; }
    public ResourceSpec getResources() { return resources; }
    public void setScript(String script) { this.script = script; }
    public void setResources(ResourceSpec resources) { this.resources = resources; }
}
