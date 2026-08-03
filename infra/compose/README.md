# Local development data plane

> **Warning:** This Compose stack is only for a trusted, single-developer workstation.
> It disables OpenSearch security and does not provide production-grade TLS, secret
> management, hardening, or isolation. **Never run it on an Internet-facing or shared
> host.** Use the managed AWS data plane for deployed environments.

## Setup

1. Create the ignored local environment file:

   ```bash
   make env-example
   ```

2. Edit `infra/env/.env` and replace every `CHANGE_ME` credential with a unique local
   value. PostgreSQL and Redis refuse to start through `make infra-up` while template
   credentials remain.
3. Start the stack:

   ```bash
   make infra-up
   ```

The Make target passes `infra/env/.env` to Compose explicitly. If invoking Compose
directly, preserve that behavior:

```bash
docker compose --env-file infra/env/.env up -d
```

PostgreSQL (`5432`), authenticated Redis (`6379`), and LocalStack (`4566`) bind only to
`127.0.0.1` for host-run application services. Unsecured OpenSearch is available only
on the `taskmind` Compose network; inspect it from inside the network, for example:

```bash
docker compose --env-file infra/env/.env exec opensearch \
  curl -fsS http://localhost:9200/_cluster/health
```

LocalStack runs only S3 for TaskMind and does not receive the Docker daemon socket.
