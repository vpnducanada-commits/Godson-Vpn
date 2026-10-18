#!/usr/bin/env python3
"""
GODSON VPN — backend v1 (contrat exact de app/src/main/java/com/godson/vpn/api).

Python STANDARD UNIQUEMENT (aucun pip) — conçu pour tourner sur un petit VPS
Ubuntu/Debian derrière un proxy TLS (Caddy).

Endpoints (miroir de GodsonApi.kt) :
  GET    api/v1/health            -> {"status","version"}
  GET    api/v1/servers           -> {"servers":[ServerDto]}
  POST   api/v1/vpn/session       -> VpnSessionResponse  (body {serverId, clientPublicKey})
  DELETE api/v1/vpn/session/{id}  -> 204

Sécurité :
  - ne voit JAMAIS de clé privée (le client n'envoie que sa clé publique WireGuard) ;
  - n'écrit aucune clé dans les journaux ;
  - s'appuie sur `wg set wg0 peer ...` (outillage officiel WireGuard) pour gérer
    les pairs à la volée — une session = un pair avec /32 dédié.

Mode essai sans WireGuard : GODSON_WG_DRY_RUN=1 (les commandes `wg` sont
affichées au lieu d'être exécutées — utilisé pour les tests d'intégration).
"""
import base64
import json
import os
import subprocess
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SERVERS_FILE = os.environ.get("GODSON_SERVERS_FILE", os.path.join(BASE_DIR, "servers.json"))
SESSIONS_FILE = os.environ.get("GODSON_SESSIONS_FILE", os.path.join(BASE_DIR, "sessions.json"))
WG_PUBKEY_FILE = os.environ.get("GODSON_WG_PUBLIC_KEY_FILE", "/etc/wireguard/server.pub")
WG_INTERFACE = os.environ.get("GODSON_WG_INTERFACE", "wg0")
WG_DRY_RUN = os.environ.get("GODSON_WG_DRY_RUN", "0") == "1"
TUNNEL_NET = os.environ.get("GODSON_TUNNEL_NET", "10.8.0.")  # .1 = serveur, .2-.254 = clients
DNS_IN_TUNNEL = os.environ.get("GODSON_DNS", "10.8.0.1, 1.1.1.1, 1.0.0.1")
KEEPALIVE = 25
VERSION = "1.0.0"

_lock = threading.Lock()
_sessions = {}  # id -> {"ip","pubkey","serverId","created"}


def _load_sessions():
    if os.path.exists(SESSIONS_FILE):
        try:
            with open(SESSIONS_FILE) as f:
                _sessions.update(json.load(f))
        except Exception as e:
            print(f"[godson] sessions.json illisible ({e}) — démarrage à vide", flush=True)


def _persist():
    tmp = SESSIONS_FILE + ".tmp"
    with open(tmp, "w") as f:
        json.dump(_sessions, f)
    os.replace(tmp, SESSIONS_FILE)


def _wg(*args):
    cmd = ["wg", "set", WG_INTERFACE, *args]
    if WG_DRY_RUN:
        print("[godson][dry-run] " + " ".join(cmd), flush=True)
        return True
    return subprocess.call(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) == 0


def _read_servers():
    with open(SERVERS_FILE) as f:
        data = json.load(f)
    return data["servers"] if isinstance(data, dict) else data


def _server_public_key():
    with open(WG_PUBKEY_FILE) as f:
        return f.read().strip()


def _valid_pubkey(value):
    try:
        raw = base64.b64decode(value, validate=True)
        return len(raw) == 32
    except Exception:
        return False


def _next_client_ip():
    used = {s["ip"] for s in _sessions.values()}
    for last in range(2, 255):
        ip = f"{TUNNEL_NET}{last}"
        if ip not in used:
            return ip
    return None


class Handler(BaseHTTPRequestHandler):
    server_version = "godson-backend/" + VERSION
    protocol_version = "HTTP/1.1"

    # ── helpers ──────────────────────────────────────────────────────────
    def _send(self, code, payload=None):
        body = b"" if payload is None else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if body:
            self.wfile.write(body)

    def _error(self, code, http_status, message):
        self._send(http_status, {"error": {"code": code, "message": message}})

    def log_message(self, fmt, *args):
        # Aucune donnée sensible : chemins et codes uniquement (pas de corps).
        print(f"[godson] {self.address_string()} {fmt % args}", flush=True)

    # ── routing ──────────────────────────────────────────────────────────
    def do_GET(self):
        path = self.path.split("?", 1)[0].strip("/")
        if path == "api/v1/health":
            return self._send(200, {"status": "ok", "version": VERSION})
        if path == "api/v1/servers":
            try:
                return self._send(200, {"servers": _read_servers()})
            except Exception as e:
                return self._error("SERVERS_UNAVAILABLE", 503, f"Catalogue indisponible : {e}")
        return self._error("NOT_FOUND", 404, "Ressource inconnue")

    def do_POST(self):
        path = self.path.split("?", 1)[0].strip("/")
        if path != "api/v1/vpn/session":
            return self._error("NOT_FOUND", 404, "Ressource inconnue")
        try:
            length = int(self.headers.get("Content-Length", "0"))
            body = json.loads(self.rfile.read(length) or b"{}")
        except Exception:
            return self._error("INVALID_REQUEST", 400, "Corps JSON illisible")
        server_id = body.get("serverId")
        client_pub = body.get("clientPublicKey")
        if not server_id or not client_pub:
            return self._error("INVALID_REQUEST", 400, "serverId et clientPublicKey requis")
        if not _valid_pubkey(client_pub):
            return self._error("INVALID_PUBLIC_KEY", 400, "Clé publique WireGuard invalide (32 octets base64 attendus)")

        server = next((s for s in _read_servers() if s.get("id") == server_id), None)
        if server is None:
            return self._error("SERVER_NOT_FOUND", 404, "Serveur inconnu")
        if server.get("status") not in ("AVAILABLE", "BUSY"):
            return self._error("SERVER_UNAVAILABLE", 409, "Serveur non connectable")

        with _lock:
            ip = _next_client_ip()
            if ip is None:
                return self._error("NO_CAPACITY", 503, "Capacité du serveur atteinte")
            if not _wg("peer", client_pub, "allowed-ips", f"{ip}/32"):
                return self._error("WG_ERROR", 502, "Application du pair impossible (WireGuard)")
            session_id = str(uuid.uuid4())
            _sessions[session_id] = {
                "ip": ip,
                "pubkey": client_pub,
                "serverId": server_id,
                "created": int(time.time()),
            }
            try:
                _persist()
            except Exception as e:
                print(f"[godson] persistance impossible : {e}", flush=True)

        return self._send(200, {
            "sessionId": session_id,
            "address": f"{ip}/32",
            "dns": DNS_IN_TUNNEL,
            "serverPublicKey": _server_public_key(),
            "endpointHost": server["hostname"],
            "endpointPort": int(server.get("port", 51820)),
            "allowedIps": ["0.0.0.0/0", "::/0"],
            "persistentKeepaliveSeconds": KEEPALIVE,
        })

    def do_DELETE(self):
        path = self.path.split("?", 1)[0].strip("/")
        parts = path.split("/")
        if len(parts) != 5 or parts[:4] != ["api", "v1", "vpn", "session"]:
            return self._error("NOT_FOUND", 404, "Ressource inconnue")
        session_id = parts[4]
        with _lock:
            session = _sessions.pop(session_id, None)
            if session is None:
                return self._error("SESSION_NOT_FOUND", 404, "Session inconnue ou déjà close")
            _wg("peer", session["pubkey"], "remove")
            try:
                _persist()
            except Exception:
                pass
        return self._send(204)


if __name__ == "__main__":
    _load_sessions()
    host = os.environ.get("GODSON_BIND", "127.0.0.1")
    port = int(os.environ.get("GODSON_PORT", "8087"))
    print(f"[godson] backend v{VERSION} sur {host}:{port} (dry-run={WG_DRY_RUN})", flush=True)
    ThreadingHTTPServer((host, port), Handler).serve_forever()
