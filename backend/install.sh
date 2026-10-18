#!/usr/bin/env bash
# GODSON VPN — installateur backend en UNE commande (VPS Ubuntu 22.04/24.04, Debian 12).
#
# Usage (en root sur le VPS, après `git clone` du dépôt) :
#   sudo bash backend/install.sh <ton-domaine.duckdns.org> [port-wireguard]
#
# Fait : WireGuard (paire de clés serveur, wg0, NAT), backend HTTP (systemd),
# Caddy avec HTTPS automatique (Let's Encrypt), pare-feu, catalogue pointé sur
# <domaine>. Ne collecte rien, ne journalise aucune clé.
set -euo pipefail

DOMAIN="${1:-}"
WG_PORT="${2:-51820}"
[ -z "$DOMAIN" ] && { echo "Usage : sudo bash backend/install.sh <domaine> [port-wg]"; exit 1; }
[ "$(id -u)" -ne 0 ] && { echo "À exécuter en root (sudo)."; exit 1; }
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "── 1/7 Paquets ──"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq wireguard-tools caddy python3 curl jq >/dev/null

echo "── 2/7 Routage + pare-feu ──"
sysctl -w net.ipv4.ip_forward=1 >/dev/null
echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-godson.conf
DEV="$(ip route show default | awk '{print $5; exit}')"
if command -v ufw >/dev/null; then
  ufw allow 51820/udp >/dev/null 2>&1 || true
  ufw allow 80/tcp  >/dev/null 2>&1 || true
  ufw allow 443/tcp >/dev/null 2>&1 || true
fi

echo "── 3/7 WireGuard (wg0, clés serveur) ──"
[ -f /etc/wireguard/server.key ] || { wg genkey | tee /etc/wireguard/server.key | wg pubkey > /etc/wireguard/server.pub; chmod 600 /etc/wireguard/server.key; }
WG_PUB="$(cat /etc/wireguard/server.pub)"
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
Address = 10.8.0.1/24
ListenPort = ${WG_PORT}
PrivateKey = $(cat /etc/wireguard/server.key)
PostUp = iptables -t nat -A POSTROUTING -o ${DEV} -j MASQUERADE
PostDown = iptables -t nat -D POSTROUTING -o ${DEV} -j MASQUERADE
EOF
systemctl enable --now wg-quick@wg0 >/dev/null 2>&1 || systemctl restart wg-quick@wg0

echo "── 4/7 Backend (systemd, port local 8087) ──"
mkdir -p /opt/godson-backend
cp "${REPO_DIR}/backend/server.py"   /opt/godson-backend/
cp "${REPO_DIR}/backend/servers.json" /opt/godson-backend/
sed -i "s|REMPLACE_PAR_INSTALLATEUR.example.duckdns.org|${DOMAIN}|; s|\"port\": 51820|\"port\": ${WG_PORT}|" /opt/godson-backend/servers.json
cat > /etc/systemd/system/godson-backend.service <<EOF
[Unit]
Description=Godson VPN backend v1
After=network-online.target wg-quick@wg0.service
[Service]
ExecStart=/usr/bin/python3 /opt/godson-backend/server.py
Environment=GODSON_BIND=127.0.0.1
Environment=GODSON_PORT=8087
Environment=GODSON_WG_PUBLIC_KEY_FILE=/etc/wireguard/server.pub
Environment=GODSON_WG_INTERFACE=wg0
Environment=GODSON_SESSIONS_FILE=/var/lib/godson/sessions.json
RuntimeDirectory=godson
Restart=always
[Install]
WantedBy=multi-user.target
EOF
mkdir -p /var/lib/godson
systemctl daemon-reload
systemctl enable --now godson-backend >/dev/null 2>&1 || systemctl restart godson-backend

echo "── 5/7 HTTPS automatique (Caddy → 127.0.0.1:8087) ──"
cat > /etc/caddy/Caddyfile <<EOF
${DOMAIN} {
    reverse_proxy 127.0.0.1:8087
}
EOF
systemctl enable --now caddy >/dev/null 2>&1 || systemctl restart caddy

echo "── 6/7 Auto-vérification ──"
sleep 2
HEALTH="$(curl -s --max-time 10 "https://${DOMAIN}/api/v1/health" || true)"
echo "health : ${HEALTH:-<pas encore joignable — vérifie le DNS et relance ce script>}"
SERVERS="$(curl -s --max-time 10 "https://${DOMAIN}/api/v1/servers" || true)"
echo "servers: $(echo "$SERVERS" | jq -c '.servers[0].id' 2>/dev/null || echo '<erreur>')"

echo "── 7/7 Terminé ──"
echo "Clé publique WireGuard du serveur : ${WG_PUB}"
echo "Backend : https://${DOMAIN}  (health, servers, vpn/session)"
echo "Prochaine étape : donner l'URL https://${DOMAIN} pour construire l'APK en mode RÉEL."
