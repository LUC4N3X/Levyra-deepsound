# Levyra Canvas Resolver Deployment Guide

This document describes the production deployment topology, security configuration, and operational verification for the on-demand Levyra Canvas Resolver service.

## Architecture

The on-demand Canvas Resolver runs as an internal daemon behind a TLS-terminating reverse proxy (such as Caddy, Nginx, or Cloudflare).

- Inbound requests: Android client queries `https://canvas.levyra.org/v1/resolve` via HTTPS.
- Reverse Proxy: Terminates TLS, enforces DDoS protection, provides primary edge rate limiting, restricts HTTP methods (`POST` for `/v1/resolve`, `GET` for `/health`), and forwards requests to the Python service.
- Python Backend: Runs `python -m levyra_editorial.resolver`, handles caching (in-memory L1 and persistent SQLite L2), deduplicates concurrent queries, coordinates upstream Spotify/PaxSenix lookups, validates MP4 Canvas links, and records discoveries.
- Storage: Persistent volume mounted at `/var/lib/levyra/canvas_cache.db` storing positive/negative cache entries and canonical discoveries.

## Cache and Service Defaults

The service implements bounded caching and strict timeouts:

- Positive cache TTL: 24 hours (86,400 seconds).
- Negative cache TTL: 10 minutes (600 seconds).
- In-memory L1 cache capacity: 50,000 entries (LRU).
- Persistent L2 storage: SQLite in WAL mode with indexed expiry, automatic purging on write, and discoveries capped at 10,000 entries.
- Cache reloading: Entries promoted from L2 to L1 retain their exact remaining TTL rather than resetting to a fresh duration.
- Maximum request body: 16 KB.
- Socket read timeout: 10.0 seconds.
- Reverse proxy trust: `X-Forwarded-For` and `X-Real-IP` are inspected only when the direct connection originates from a loopback peer (`127.0.0.1` or `::1`). Direct non-loopback connections always use the peer socket address.

## Environment Variables

The service is configured using standard environment variables:

| Variable | Required | Default | Description |
|---|---|---|---|
| `LEVYRA_EDITORIAL_SP_DC` | Yes | (empty) | Spotify `sp_dc` cookie for server-side token authentication. Never exposed to clients. |
| `LEVYRA_RESOLVER_HOST` | No | `127.0.0.1` | Local interface binding address. |
| `LEVYRA_RESOLVER_PORT` | No | `8080` | Internal TCP listening port. |
| `LEVYRA_RESOLVER_CACHE_DB` | No | `canvas_cache.db` | Path to SQLite L2 cache database. |
| `LEVYRA_RESOLVER_RATE_LIMIT` | No | `60` | Internal per-IP rate limit per minute fallback. |

## Systemd Service

Create the service file `/etc/systemd/system/levyra-canvas-resolver.service`:

```ini
[Unit]
Description=Levyra Canvas Resolver Service
After=network.target

[Service]
Type=simple
User=levyra
Group=levyra
WorkingDirectory=/opt/levyra/tools/levyra-editorial
Environment=PYTHONPATH=/opt/levyra/tools/levyra-editorial
Environment=LEVYRA_RESOLVER_HOST=127.0.0.1
Environment=LEVYRA_RESOLVER_PORT=8080
Environment=LEVYRA_RESOLVER_CACHE_DB=/var/lib/levyra/canvas_cache.db
Environment=LEVYRA_RESOLVER_RATE_LIMIT=60
EnvironmentFile=/etc/levyra/resolver.env
ExecStart=/usr/bin/python3 -m levyra_editorial.resolver
Restart=always
RestartSec=5
LimitNOFILE=65536
PrivateTmp=true
ProtectSystem=full
ProtectHome=true

[Install]
WantedBy=multi-user.target
```

Create `/etc/levyra/resolver.env` with restricted permissions (`chmod 600`):

```text
LEVYRA_EDITORIAL_SP_DC=your_sp_dc_cookie_here
```

## Reverse Proxy Configuration

The resolver backend expects to run behind a local reverse proxy (Caddy or Nginx) terminating TLS.

### Trusted Proxy Contract

To prevent rate-limit identity spoofing:
1. The resolver backend processes forwarded headers only when the direct connecting peer is a trusted loopback address (`127.0.0.1` or `::1`). Direct non-loopback clients cannot spoof client identity with arbitrary headers.
2. The trusted edge proxy MUST overwrite `X-Forwarded-For` and `X-Real-IP` with the direct client remote address (`{remote_host}` or `$remote_addr`). Appending proxy configurations (such as `$proxy_add_x_forwarded_for`) MUST NOT be used because client-supplied headers could be preserved.

### Caddy Example

```caddy
canvas.levyra.org {
    encode gzip zstd

    @health {
        method GET
        path /health /v1/health
    }
    handle @health {
        reverse_proxy 127.0.0.1:8080
    }

    @resolve {
        method POST
        path /v1/resolve
    }
    handle @resolve {
        request_body {
            max_size 16KB
        }
        reverse_proxy 127.0.0.1:8080 {
            header_up X-Real-IP {remote_host}
            header_up X-Forwarded-For {remote_host}
        }
    }

    handle {
        respond "Not Found" 404
    }
}
```

### Nginx Example

```nginx
server {
    listen 443 ssl http2;
    server_name canvas.levyra.org;

    ssl_certificate /etc/letsencrypt/live/canvas.levyra.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/canvas.levyra.org/privkey.pem;

    client_max_body_size 16k;

    location = /health {
        limit_except GET { deny all; }
        proxy_pass http://127.0.0.1:8080/health;
    }

    location = /v1/health {
        limit_except GET { deny all; }
        proxy_pass http://127.0.0.1:8080/v1/health;
    }

    location = /v1/resolve {
        limit_except POST { deny all; }
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_pass http://127.0.0.1:8080/v1/resolve;
    }

    location / {
        return 404;
    }
}
```

## Production Verification

Run the following checks against the deployed service:

### 1. Health Verification

```bash
curl -sS -i https://canvas.levyra.org/health
```

Expected output:
```http
HTTP/2 200
content-type: application/json

{"status":"ok"}
```

### 2. Method Enforcement Verification

```bash
curl -sS -i -X GET https://canvas.levyra.org/v1/resolve
```

Expected output:
```http
HTTP/2 404
```

### 3. Sanitized Resolution Verification

```bash
curl -sS -X POST https://canvas.levyra.org/v1/resolve \
  -H "Content-Type: application/json" \
  -d '{"isrc":"USUM71703861","title":"HUMBLE.","artist":"Kendrick Lamar"}'
```

Expected output:
```json
{"status":"resolved","url":"https://canvaz.scdn.co/upload/artist/77334789/video/449622d1ad2f4e04be240954b9d0dcda.cnvs.mp4"}
```

### 4. Credential Leak Verification

Inspect headers and response body to ensure no authorization tokens, session cookies, or Spotify internals are returned.
