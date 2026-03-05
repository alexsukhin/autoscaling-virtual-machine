package com.jetbrains.executor.controller;

import com.jetbrains.executor.model.Execution;
import com.jetbrains.executor.model.ExecutionRequest;
import com.jetbrains.executor.service.ShellExecutorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/executions")
public class ExecutionController {

    private final ShellExecutorService executorService;

    public ExecutionController(ShellExecutorService executorService) {
        this.executorService = executorService;
    }

    @PostMapping
    public ResponseEntity<Execution> submit(@RequestBody ExecutionRequest request) {
        if (request.getScript() == null || request.getScript().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Execution execution = executorService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(execution);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Execution> getById(@PathVariable String id) {
        Execution execution = executorService.get(id);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(execution);
    }

    @GetMapping
    public ResponseEntity<Collection<Execution>> getAll() {
        return ResponseEntity.ok(executorService.getAll());
    }
}