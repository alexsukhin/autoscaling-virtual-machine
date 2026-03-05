# Shell Executor

A REST service that accepts shell scripts, runs them inside isolated Docker containers, and tracks their execution status.

## How it works

1. You POST a script with optional resource requirements (CPU count, memory)
2. The service spins up a fresh Alpine Linux container with those constraints applied
3. Your script runs inside it
4. You can poll the execution status: `QUEUED → IN_PROGRESS → FINISHED`

Docker acts as the remote executor - each execution gets its own container, which is created on demand, used, then removed.

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker Engine

## Setup

### Install Java 21

```bash
sudo apt install openjdk-21-jdk
```

### Install Maven

```bash
sudo apt install maven
```

### Install Docker

```bash
sudo apt-get update
sudo apt-get install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io

sudo usermod -aG docker $USER
newgrp docker
```

### Enable Docker TCP access

The service connects to Docker over TCP. Enable it by adding a TCP listener:

```bash
sudo systemctl edit docker
```

Add the following:

```
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://127.0.0.1:2375
```

Then restart Docker:

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## Running

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

## API

### Submit a command

```
POST /executions
```

```json
{
  "script": "echo hello && uname -a",
  "resources": {
    "cpuCount": 2,
    "memoryMb": 256
  }
}
```

Returns the created execution with its `id` and initial status.

### Get execution status

```
GET /executions/{id}
```

Returns the execution including current `status`, `output`, and `error`.

### List all executions

```
GET /executions
```

## Example

**Submit:**
```bash
curl -s -X POST http://localhost:8080/executions \
  -H "Content-Type: application/json" \
  -d '{
    "script": "echo hello from docker && uname -a",
    "resources": { "cpuCount": 1, "memoryMb": 128 }
  }' | python3 -m json.tool
```

**Poll status:**
```bash
curl -s http://localhost:8080/executions/<id> | python3 -m json.tool
```

**Example finished response:**
```json
{
  "id": "a3f1bc92",
  "script": "echo hello from docker && uname -a",
  "resources": {
    "cpuCount": 1,
    "memoryMb": 128
  },
  "status": "FINISHED",
  "output": "hello from docker\nLinux ...",
  "error": "",
  "createdAt": "2024-03-05T12:00:00Z"
}
```

---
