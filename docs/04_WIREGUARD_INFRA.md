# GODSON VPN — Infrastructure WireGuard & Backend (Phase 4/5)

Ce document liste **précisément** ce qui exige un VPS, un domaine et des clés WireGuard,
et donne les étapes exactes de mise en place. Rien de ce qui est décrit ici n'existe
encore : l'application est en mode DÉMO tant que ces étapes ne sont pas exécutées.

---

## 1. Vue d'ensemble

```
Appareil Android (client)
  │  clé publique du client ; clé privée JAMAIS exportée
  │
  │  HTTPS (API)                        WireGuard/UDP 51820
  ▼                                        ▼
Backend API (api.votredomaine.tld)     Serveur(s) VPN WireGuard
  GET /api/v1/health                   1 interface wg0 par serveur
  GET /api/v1/servers                  1 pair par appareil (révocable)
  POST /api/v1/vpn/session             NAT/forward activés
  DELETE /api/v1/vpn/session/{id}
```

Règle d'or : **le backend ne touche jamais au trafic** ; il ne fait que décrire les
serveurs et délivrer des configurations éphémères. Le trafic utilisateur transite
directement par WireGuard (UDP).

## 2. Ce que VOUS devez fournir (action humaine)

| # | Ressource | Détails | Coût indicatif |
|---|---|---|---|
| 1 | **1..N VPS** | Ubuntu 24.04 LTS, 1 vCPU / 1 Go mini, IPv4 publique, UDP 51820 ouvert | 3-10 €/mois chacun |
| 2 | **Un domaine** | ex. `godsonvpn.tld` + enregistrement A `api.` et par serveur | ~10 €/an |
| 3 | **Un hébergement backend** | peut être le même VPS (petite charge) ou un PaaS | 0-5 €/mois |
| 4 | **Génération de clés** | `wg genkey`/`wg pubkey` — 1 paire PAR SERVEUR (jamais partagée) | gratuit |
| 5 | Certificats TLS | Let's Encrypt (automatique via Caddy) | gratuit |

## 3. Serveur WireGuard — étapes exactes (par VPS)

```bash
# 1. Installation
sudo apt update && sudo apt install -y wireguard ufw

# 2. Clés DU SERVEUR (une paire par serveur, jamais réutilisée)
umask 077
wg genkey | sudo tee /etc/wireguard/server.key | wg pubkey | sudo tee /etc/wireguard/server.pub

# 3. Configuration /etc/wireguard/wg0.conf
[Interface]
Address    = 10.8.0.1/24          # réseau du tunnel (unique par serveur)
ListenPort = 51820
PrivateKey = <contenu de /etc/wireguard/server.key>
# DNS du tunnel : votre résolveur (ex. Unbound local) — JAMAIS celui du FAI
PostUp     = echo "nameserver 10.8.0.1" >/dev/null || true
# NAT (remplacer eth0 par l'interface réelle : `ip route | grep default`)
PostUp   = iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

# 4. Routage + pare-feu
sudo sysctl -w net.ipv4.ip_forward=1
echo 'net.ipv4.ip_forward=1' | sudo tee /etc/sysctl.d/99-wg.conf
sudo ufw allow 51820/udp && sudo ufw allow OpenSSH && sudo ufw enable

# 5. Démarrage
sudo systemctl enable --now wg-quick@wg0
sudo wg show   # vérification
```

**Ajout d'un client** (par le backend à chaque session) :
`wg set wg0 peer <CLÉ_PUBLIQUE_CLIENT> allowed-ips 10.8.0.X/32` — la révocation
consiste à retirer le pair (`wg set wg0 peer <clé> remove`), base de la gestion
d'accès par session éphémère.

**Pour un pays « Disponible »** : ajouter le serveur au backend (`servers`), avec
statut `AVAILABLE`, ville, hostname, port. Un pays sans serveur configuré reste
« Bientôt disponible » dans l'application — comportement déjà implémenté.

## 4. Contrat API v1 (implémenté côté client, à implémenter côté serveur)

| Route | Méthode | Corps / Réponse |
|---|---|---|
| `/api/v1/health` | GET | `{"status":"ok","version":"1.0"}` |
| `/api/v1/servers` | GET | `{"servers":[{"id","countryCode","city","hostname","port","status","pingMs?","loadPercent?","recommended"}]}` — `status` ∈ AVAILABLE/BUSY/UNAVAILABLE/PLANNED |
| `/api/v1/vpn/session` | POST | req `{"serverId","clientPublicKey"}` → resp `{"sessionId","address":"10.8.0.7/32","dns":"10.8.0.1","serverPublicKey","endpointHost","endpointPort","allowedIps":["0.0.0.0/0","::/0"],"persistentKeepaliveSeconds":25}` |
| `/api/v1/vpn/session/{id}` | DELETE | 204 — retire le pair WireGuard (révocation immédiate) |

Erreurs : `{"error":{"code":"SERVER_UNAVAILABLE","message":"…"}}`, codes HTTP 4xx/5xx,
rate limiting par IP/jeton. Auth : jeton d'appareil anonyme (header
`Authorization: Bearer <device_token>`) à ajouter côté client en Phase 4 (intercepteur
OkHttp déjà prévu dans `ApiClient.kt`).

## 5. Domaine + TLS

```bash
# Caddy gère Let's Encrypt automatiquement
sudo apt install -y caddy
# /etc/caddy/Caddyfile :
#   api.godsonvpn.tld {
#       reverse_proxy 127.0.0.1:8000
#   }
sudo systemctl reload caddy
```

## 6. Secrets — règle absolue

- **Jamais** dans l'APK : clés privées serveur, jetons d'administration, mots de passe.
- Côté serveur uniquement : variables d'environnement (`/etc/godson-backend.env`,
  permissions 600) ou gestionnaire de secrets.
- Côté client : uniquement l'URL publique de l'API (dans `app/build.gradle.kts`,
  `API_BASE_URL`) et ses propres clés WireGuard locales (`VpnKeyManager`).

## 7. Basculer l'application en mode RÉEL

1. Exécuter les étapes §3-§5.
2. Dans `android/app/build.gradle.kts`, remplacer :
   `buildConfigField("String", "API_BASE_URL", "\"https://api.godsonvpn.example.invalid\"")`
   par l'URL réelle (debug ET release).
3. Rebuild → `AppMode` passe automatiquement en RÉEL (détection `.invalid`), l'UI
   retire les badges DÉMO, et « Connecter » déclenche le vrai cycle WireGuard.

## 8. Checklist sécurité avant ouverture au public

- [ ] Pare-feu : seul 51820/udp, 22 (restreint), 80/443 ouverts.
- [ ] Backend : rate limiting, jetons révocables, journaux minimaux (pas d'IP après 24 h).
- [ ] Tests de fuite DNS/IPv6 depuis un appareil réel (docs/05_BUILD_AND_TEST.md §7).
- [ ] Sauvegardes des configurations serveur (hors clés privées clients).
- [ ] Mise à jour automatique de sécurité (unattended-upgrades).
