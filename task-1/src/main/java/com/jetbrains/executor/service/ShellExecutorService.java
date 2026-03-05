package com.jetbrains.executor.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.jetbrains.executor.model.Execution;
import com.jetbrains.executor.model.ExecutionRequest;
import com.jetbrains.executor.model.ResourceSpec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ShellExecutorService {

    private final Map<String, Execution> executions = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final DockerClient docker;

    public ShellExecutorService() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(30))
                .build();
        this.docker = DockerClientImpl.getInstance(config, httpClient);
    }

    public Execution submit(ExecutionRequest request) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        ResourceSpec resources = request.getResources() != null
                ? request.getResources()
                : new ResourceSpec();

        Execution execution = new Execution(id, request.getScript(), resources);
        executions.put(id, execution);

        threadPool.submit(() -> run(execution));

        return execution;
    }

    public Execution get(String id) {
        return executions.get(id);
    }

    public Collection<Execution> getAll() {
        return executions.values();
    }

    private void run(Execution execution) {
        String containerId = null;
        try {
            execution.setStatus(Execution.Status.IN_PROGRESS);

            docker.pullImageCmd("alpine").withTag("latest")
                    .start().awaitCompletion();

            long nanoCpus = (long) execution.getResources().getCpuCount() * 1_000_000_000L;
            long memoryBytes = (long) execution.getResources().getMemoryMb() * 1024 * 1024;

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNanoCPUs(nanoCpus)
                    .withMemory(memoryBytes);

            CreateContainerResponse container = docker.createContainerCmd("alpine")
                    .withCmd("sh", "-c", execution.getScript())
                    .withHostConfig(hostConfig)
                    .exec();

            containerId = container.getId();
            docker.startContainerCmd(containerId).exec();

            docker.waitContainerCmd(containerId).start().awaitCompletion();

            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();

            docker.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String line = new String(frame.getPayload());
                            if (frame.getStreamType() == StreamType.STDERR) {
                                errors.append(line);
                            } else {
                                output.append(line);
                            }
                        }
                    })
                    .awaitCompletion();

            execution.setOutput(output.toString().trim());
            execution.setError(errors.toString().trim());
            execution.setStatus(Execution.Status.FINISHED);

        } catch (Exception e) {
            execution.setError(e.getMessage());
            execution.setStatus(Execution.Status.FAILED);
        } finally {
            if (containerId != null) {
                try {
                    docker.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        }
    }
}
