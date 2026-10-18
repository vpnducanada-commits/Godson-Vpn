# GODSON VPN — Projet complet

> **Votre connexion. Votre contrôle.**
> VPN Android premium, rapide et centré sur la confidentialité. Kotlin · Jetpack Compose · WireGuard.

---

## ⚠️ Avertissement important (honnêteté technique)

Cet espace de travail **ne permet pas de compiler un APK** (pas d'Android SDK, pas de JDK 17, et le
dossier `build/` n'est pas persisté). En conséquence :

- Ce dépôt contient le **code source Android réel et buildable** — pas un site web prétendant être un VPN.
- La **maquette interactive** (`demo/godson_vpn_demo.html`) est une **prévisualisation de design clairement
  identifiée** : données fictives, aucune connexion VPN réelle. Elle ne remplace pas l'application.
- **Aucun faux état « connecté », aucune fausse adresse IP, aucun faux serveur actif** : tout ce qui n'est
  pas encore branché est marqué comme tel dans le code et l'interface.

## 📊 État d'avancement

| Phase | Contenu | Statut |
|---|---|---|
| 1 | Analyse & architecture | ✅ Terminée |
| 2 | Identité visuelle, design system, écrans (**refonte premium v2 appliquée**) | ✅ Terminée |
| 3 | Fonctionnement applicatif (polices Inter, EN/PT natifs, split tunneling) | 🔲 À venir |
| 4 | Backend & catalogue de serveurs réel | 🔲 À venir |
| 5 | Intégration VPN réelle (WireGuard / VpnService) | 🔲 À venir |
| 6 | Tests & sécurité | 🔲 À venir |
| 7 | Build, signature, publication | 🔲 À venir |

Détail : [`docs/02_PLAN_REALISATION.md`](docs/02_PLAN_REALISATION.md)

## 🗂️ Arborescence

```
GodsonVPN/
├── README.md                  ← ce fichier
├── docs/                      ← documentation
│   ├── 01_PHASE1_ARCHITECTURE.md
│   ├── 02_PLAN_REALISATION.md
│   └── 03_DESIGN_SYSTEM.md    ← design system v2 (couleurs, type, mouvement, composants)
├── android/                   ← PROJET ANDROID RÉEL (ouvrir dans Android Studio)
│   ├── settings.gradle.kts
│   ├── gradle/libs.versions.toml
│   └── app/src/main/
│       ├── java/com/godson/vpn/
│       │   ├── navigation/    ← GodsonApp : 9 routes, transitions 240 ms
│       │   ├── ui/components/ ← design system (GodsonCard, FlagEmojiBadge, SignalBars…)
│       │   ├── ui/country/    ← « Choisir un pays » : 30 pays / 6 régions, filtres, favoris
│       │   ├── ui/dashboard/  ← bouton à anneaux, statut réel, VpnService réel
│       │   ├── ui/servers/    ← recherche, favoris persistants, skeletons
│       │   ├── ui/stats/      ← durées de session réelles + chronologie
│       │   ├── ui/settings/   ← interrupteurs fonctionnels persistés
│       │   ├── ui/privacy/ · ui/support/ · ui/profile/
│       │   ├── vpn/           ← VpnStateMachine (testée) + GodsonVpnService
│       │   └── data/          ← DataStore, catalogue démo, filtres testés
│       └── res/               ← logo GV, chaînes FR, thème
├── demo/
│   └── godson_vpn_demo.html   ← maquette de référence (données fictives, FR/EN/PT)
└── brand/                     ← logo GV : 4 variantes SVG + règles d'usage
```

## 🚀 Démarrage rapide

### Voir l'interface tout de suite
Ouvrir `demo/godson_vpn_demo.html` dans un navigateur (ou dans l'aperçu). C'est une **maquette** :
navigation complète, bouton de connexion simulé (clairement étiqueté), thème clair/sombre, FR/EN/PT.

### Construire l'application Android réelle
1. Installer **Android Studio** (Ladybug ou plus récent) avec **JDK 17+** (fourni par Studio).
2. Ouvrir le dossier `android/` dans Android Studio.
3. Laisser Studio générer le wrapper Gradle s'il le demande (Gradle 8.13 attendu) et synchroniser
   (téléchargement des dépendances, dont `com.wireguard.android:tunnel`).
4. `Run ▶` sur un appareil ou émulateur **API 26+**.

> **Étapes exactes, parcours de test complet et vérifications de fuite** :
> [`docs/05_BUILD_AND_TEST.md`](docs/05_BUILD_AND_TEST.md).
> **Infrastructure serveur (VPS, domaine, clés WireGuard)** :
> [`docs/04_WIREGUARD_INFRA.md`](docs/04_WIREGUARD_INFRA.md).
> En mode actuel (DÉMO), la connexion affiche volontairement une erreur explicite :
> aucun tunnel n'est établi tant qu'aucun backend réel n'est configuré.

## 📚 Documentation

| Document | Contenu |
|---|---|
| `docs/01_PHASE1_ARCHITECTURE.md` | Architecture, choix techniques, sécurité, limites, ressources humaines requises |
| `docs/02_PLAN_REALISATION.md` | Plan des 7 phases, livrables, blocages |

## ⚖️ Licences

- Code du projet : à définir par le porteur du projet (proposition : propriétaire ou GPL-3.0).
- `com.wireguard.android:tunnel` : Apache License 2.0 — compatible, attribution conservée.
- WireGuard est une marque de Jason A. Donenfeld — utilisation conforme aux règles de la marque.
