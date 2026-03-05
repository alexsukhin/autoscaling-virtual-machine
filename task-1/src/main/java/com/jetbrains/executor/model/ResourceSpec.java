package com.jetbrains.executor.model;

public class ResourceSpec {

    private int cpuCount;
    private int memoryMb;

    public ResourceSpec() {
        this.cpuCount = 1;
        this.memoryMb = 512;
    }

    public ResourceSpec(int cpuCount, int memoryMb) {
        this.cpuCount = cpuCount;
        this.memoryMb = memoryMb;
    }

    public int getCpuCount() { return cpuCount; }
    public int getMemoryMb() { return memoryMb; }
    public void setCpuCount(int cpuCount) { this.cpuCount = cpuCount; }
    public void setMemoryMb(int memoryMb) { this.memoryMb = memoryMb; }
}
