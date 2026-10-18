# 09 — Passer en VPN RÉEL : VPS + backend en une commande (depuis un téléphone)

> Objectif : voir un **vrai « Connecté »** avec un vrai tunnel WireGuard chiffré.
> Tout le code (app + backend) est prêt et testé ; il reste à louer un petit serveur
> et à y coller **une seule commande**. Coût réel minimum : un VPS (~4-5 €/mois ;
> option gratuite : Oracle Cloud « Always Free », carte requise pour vérification).
> Le nom de domaine est **gratuit** via DuckDNS.

## 1. Louer un VPS (5 min)

- N'importe quel Ubuntu 22.04/24.04, 1 vCPU / 1 Go RAM suffit (Hetzner CX11, OVH,
  Contabo, Oracle Free ARM…). Cocher « IPv4 publique ».
- Noter l'**IP** + le mot de passe/la clé SSH fournis.

## 1bis. Option 100 % GRATUITE : Oracle Cloud « Always Free » (recommandée)

Vraiment gratuite à vie (pas un essai) — **carte bancaire exigée uniquement pour la
vérification d'identité** (aucun prélèvement si tu restes dans le gratuit).

- Allocation 2026 (divisée par deux en juin) : **2 cœurs ARM / 12 Go** (ou 2 micro-VM AMD
  1 Go — suffit largement : notre backend est en Python standard + WireGuard),
  200 Go de disque, **10 To de trafic/mois**.
- ⚠️ Piège n°1 : Oracle a un **double pare-feu**. Outre `ufw` (déjà géré par
  `install.sh`), il faut ouvrir les ports dans la console : Networking → ta VCN →
  Security Lists → Add Ingress Rule : `0.0.0.0/0` TCP 443, TCP 80 et **UDP 51820**.
- ⚠️ Signature-ups parfois refusés selon les régions ; capacité ARM parfois épuisée
  dans les régions populaires (réessayer plus tard, ou prendre la région proposée).
- Choisis la région la plus proche de tes utilisateurs (pour le Gabon : Europe de l'Ouest
  ou Afrique si disponible). Prends **Ubuntu 22.04/24.04** (AMD64 ou ARM64 —
  `install.sh` fonctionne sur les deux).

## 2. Nom de domaine gratuit (2 min)

1. Créer un compte sur **duckdns.org** (connexion GitHub/Google possible).
2. Créer un sous-domaine, ex. `godson-vpn` → `godson-vpn.duckdns.org`.
3. Dans la table, mettre l'**IP du VPS** comme adresse.

## 2bis. Alternative maison (vieux PC / Raspberry Pi) — seulement si IPv4 publique

Gratuit si tu possèdes déjà la machine, mais elle doit être **joignable depuis
Internet** : beaucoup de FAI (Gabon inclus) utilisent du CGNAT (pas d'IPv4 publique
dedicated). Test : l'IP affichée par « quelle est mon IP » sur ton réseau est-elle la
même que celle configurée sur ta box ? Si non (CGNAT) → cette option est bloquée sans
relais. Si oui : redirection du port UDP 51820 + TCP 443/80 vers la machine, puis
même `install.sh`.

## 3. La commande magique (5 min, depuis Termux)

```bash
pkg install -y openssh          # si pas déjà fait
ssh root@IP_DU_VPS              # accepter l'empreinte, saisir le mot de passe
```

Puis sur le VPS (une seule ligne) :

```bash
apt-get install -y git && git clone https://github.com/vpnducanada-commits/Godson-Vpn.git && sudo bash GodsonVPN/backend/install.sh godson-vpn.duckdns.org
```

Le script installe WireGuard (clés serveur, NAT), le backend HTTPS (Let's Encrypt
automatique via Caddy), le pare-feu, et s'auto-vérifie. Il affiche la **clé publique
WireGuard** du serveur à la fin — conserve-la (utile au diagnostic).

## 4. Construire l'APK en mode RÉEL

Donner l'URL `https://godson-vpn.duckdns.org` (la tienne) : le passage se fait en
**une ligne** dans `android/app/build.gradle.kts` (`API_BASE_URL`) — confié à l'assistant
Arena, qui pousse et fait construire l'APK réel par la CI. L'app bascule seule en mode
réel (voir `data/AppMode.kt`) : le catalogue affiché vient du serveur, et « Connecter »
crée une vraie session WireGuard.

## 5. Ce que tu verras alors

1. Vrai catalogue serveur servi par TON backend.
2. « Connexion sécurisée » affichée **uniquement après handshake WireGuard vérifié**
   (l'app attend la preuve, jamais avant).
3. Split tunneling opérationnel : les apps choisies sortiront hors du tunnel.
4. Statistiques réelles (octets reçus/envoyés) depuis le tunnel.

## 6. Sécurité — état et limites (honnêteté)

- ✅ TLS automatique (Let's Encrypt), clé privée client jamais transmise, aucune clé
  dans les journaux, une session = un pair /32 révoquable.
- ⚠️ v1 **sans authentification** : quiconque connaît l'URL peut créer une session
  (capacité limitée à ~253 clients par serveur). Suffisant pour un premier essai
  privé ; l'intercepteur de jeton d'appareil est prévu côté app (ApiClient, Phase 4+).
- ⚠️ 1 serveur = 1 pays. Ajouter des serveurs = ajouter des entrées dans
  `backend/servers.json` (ou d'autres VPS).
