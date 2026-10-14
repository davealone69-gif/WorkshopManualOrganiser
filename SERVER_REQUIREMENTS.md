# Server requirements and live health checks

## Service matrix

| Service | Requirement | Endpoint / detection | Start or recover |
|---|---|---|---|
| Core manual library, scanner, import/export, offline VIN decoder and wiring reference | Required app features; no network server required | Local app storage and Android camera/document-picker intents | No server to start. Grant camera access when prompted and use the system document picker. |
| Ollama local LLM | Optional. Only needed for AI Technical Help enrichment; offline knowledge-base help remains available without it | Default endpoint `http://127.0.0.1:11434/api/chat`; verify `/api/version`, then verify the configured model exists at `/api/tags` | Install Ollama and the chosen model on the same Android device or reachable host. On-device Termux: run `ollama serve`; install/pull the model separately. |
| Remote OpenAI-compatible LLM | Optional alternative if the app's configured endpoint supports it | User-configured HTTPS URL; test the endpoint and perform a minimal authenticated request | Start/deploy the provider or remote service, configure URL/model/credentials using a secure local configuration mechanism. Avoid sending vehicle/customer data to an untrusted service. |
| Cloud account, analytics or sync backend | Not required by the current README | No required endpoint identified | Do not configure one unless a future feature explicitly introduces it. |

## Runtime status requirements

If AI endpoint status is shown, perform a live network probe when the user presses **Check connection** or when opening the service panel. A non-empty URL is not a health check.

- **HEALTHY**: endpoint responds and configured model is present; preferably verify a minimal real chat request.
- **STOPPED**: local connection refused.
- **UNREACHABLE**: timeout or route failure.
- **AUTH REQUIRED**: remote provider rejects credentials.
- **DEGRADED**: endpoint responds but model/required API route is missing.
- **NOT CONFIGURED**: optional AI endpoint has not been set.
- **UNVERIFIED**: no live probe has run.

Show checked-at time, safe endpoint (no secrets), last error, and how to start/retry. Never block offline manual features because an optional AI server is down.

## Termux commands

```sh
curl -fsS --max-time 3 http://127.0.0.1:11434/api/version
curl -fsS --max-time 5 http://127.0.0.1:11434/api/tags
```

A successful version request does not prove the selected model can answer. Check the model list and send a small real chat request before reporting AI as fully healthy.

## Truth boundary

The project documentation describes a fully usable offline app with an optional LLM endpoint. This inventory does not claim that Ollama is installed, running, or reachable on any particular device.
