# GODSON VPN — PHASE 1 : Analyse & Architecture

Version : 1.0 — Phase 1 clôturée. Ce document est la référence technique du projet.
Il sépare strictement **ce qui est réel**, **ce qui est préparé**, et **ce qui exige une action humaine**.

---

## 1. Résumé exécutif

Godson VPN est une application Android native (Kotlin + Jetpack Compose) qui établit un véritable
tunnel VPN **WireGuard** via `Android VpnService`, derrière une interface premium simple et rapide,
pensée pour les téléphones d'entrée et de milieu de gamme. Le back-end gère le catalogue de serveurs
et délivre des configurations de tunnel éphémères ; le client ne contient aucun secret d'administration.

**Principe directeur :** aucune fonctionnalité n'est présentée comme active tant qu'elle n'est pas
vérifiable. En l'absence d'infrastructure, l'application l'affiche (badge « démo », valeur « indisponible »)
plutôt que de simuler un état trompeur.

---

## 2. Périmètre fonctionnel

| Priorité | Fonctionnalités |
|---|---|
| **P0 — MVP** | Splash, onboarding (3 écrans), dashboard avec bouton de connexion, machine à états VPN complète, catalogue de serveurs (recherche, favoris, tri), connexion rapide, statistiques de session, paramètres, protection contre les fuites (config DNS + routes + Always-on VPN Android), notifications d'état réelles, FR/EN/PT, thème sombre/clair/système |
| **P1 — V1** | Compte optionnel, quotas et offres configurés par le backend, split tunneling par application, vérification IP publique intégrée, export de rapport de diagnostic expurgé, Google Play Billing |
| **P2 — Évolutions** | Multi-protocole (ex. un second protocole réellement supporté), multi-hop, serveurs obfusqués, programme de parrainage |

Hors périmètre (décision) : publicité, revente de données de navigation (jamais), protocole
cryptographique maison (jamais).

---

## 3. Contraintes de la plateforme Android

| Contrainte | Traitement dans le projet |
|---|---|
| `VpnService` requiert le consentement utilisateur (dialogue système) | État dédié `AwaitingPermission` ; relance pédagogique en cas de refus |
| Android 14+ (API 34+) : service foreground typé obligatoire | Type **`specialUse`** (cas reconnu pour les VPN ; modèle : OpenVPN for Android) + permission `FOREGROUND_SERVICE_SPECIAL_USE` + propriété `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` |
| Android 13+ : permission `POST_NOTIFICATIONS` au runtime | Demande contextuelle au premier démarrage de la connexion, jamais avant |
| Doze / restrictions d'arrière-plan | Service de premier plan avec notification d'état ; documentation « Always-on VPN » système |
| Kill switch / blocage sans VPN | Délégation au mécanisme système « Always-on VPN + Block connections without VPN » ; l'app explique et propose l'ouverture des réglages (fiable, sans promesse non tenue) |
| DNS : risque de fuite | DNS poussé dans le tunnel via `VpnService.Builder.addDnsServer`, routes 0.0.0.0/0 + ::/0 ; test de fuite en Phase 6 |
| IPv6 | Routes IPv6 déclarées ; documentation du comportement en cas de réseau IPv6-only |
| Play : targetSdk 36 exigé depuis le 31/08/2026 | `compileSdk = 36`, `targetSdk = 36` dès le départ |
| Appareils modestes | minSdk 26 (Android 8.0, ≈ 99 % des appareils actifs), absence d'animations coûteuses, budget performance §10 |

---

## 4. Choix technologiques et justifications

| Choix | Justification |
|---|---|
| **Kotlin 2.1 + Jetpack Compose + Material 3 personnalisé** | Standard moderne, UI déclarative fluide, theming complet de la palette Godson |
| **MVVM + flux unidirectionnel (StateFlow)** | Séparation UI / état / logique, testable, simple à maintenir |
| **Navigation Compose** | Navigation type-safe, transitions maîtresses (fondu + 8-16 dp, 180-280 ms) |
| **DataStore Preferences** | Préférences locales asynchrones, sans risk d'ANR des SharedPreferences |
| **Retrofit + OkHttp + kotlinx.serialization** | API HTTPS typée, validation stricte, mature |
| **`com.wireguard.android:tunnel` (Apache-2.0, Maven Central, v1.0.20260102)** | Implémentation WireGuard maintenue par le projet officiel wireguard-android ; backend Go userspace (sans root). **Pas de cryptographie maison.** |
| **Injection manuelle légère (`GodsonContainer`)** | « L'option la plus sûre et la plus simple à maintenir » : pas de magie, migration vers Hilt possible plus tard si le projet grossit |
| **Module unique `:app` (Phase 1)** | Simplicité ; modularisation (`:core-ui`, `:vpn`, `:api`) prévue en Phase 3 si utile |
| **FastAPI (Python) pour le backend** (Phase 4) | Petit, typé, documenté automatiquement (OpenAPI), facile à héberger |

Dépendance critique documentée : `com.wireguard.android:tunnel` (licence Apache 2.0). Aucune autre
dépendance non documentée n'est admise.

---

## 5. Architecture logicielle

```
┌────────────────────────────────────────────────────────────┐
│  UI — Jetpack Compose (Material 3 personnalisé Godson)     │
│  Splash · Onboarding · Dashboard · Serveurs · Stats ·      │
│  Paramètres · Protection                                   │
└───────────────▲────────────────────────────────────────────┘
                │ état immuable (StateFlow), événements descendants
┌───────────────┴────────────────────────────────────────────┐
│  ViewModels — état d'écran, orchestration                  │
└──────┬──────────────────┬─────────────────────┬────────────┘
       │                  │                     │
┌──────▼───────┐   ┌──────▼───────┐    ┌────────▼─────────┐
│ VpnRepository│   │ ServerRepo   │    │ SettingsRepo     │
│ machine à    │   │ catalogue,   │    │ DataStore        │
│ états, stats │   │ ping, cache  │    │ (thème, lang…)   │
└──────┬───────┘   └──────┬───────┘    └──────────────────┘
       │                  │
┌──────▼───────────────┐  │
│ GodsonVpnService     │  │
│ (VpnService + FGS    │  │
│  specialUse)         │  │
│  ↳ GoBackend         │  │
│    (WireGuard)       │  │
└──────────────────────┘  │
                 ┌────────▼─────────┐
                 │ ApiClient        │
                 │ Retrofit/OkHttp  │
                 │ TLS /api/v1/*    │
                 └──────────────────┘
```

Règles de dépendance : l'UI ne parle jamais au réseau ni au service directement ;
les repositories sont les seules sources de vérité ; aucun secret côté client ;
les journaux ne contiennent jamais de clé privée ni d'identifiant.

---

## 6. Machine à états de la connexion VPN

États : `Disconnected → Preparing → AwaitingPermission → Connecting → Connected → Reconnecting →
Disconnecting → Disconnected`, avec `Error` et `ServerUnavailable` comme états d'échec.

| Transitions autorisées (extrait) | Règle métier |
|---|---|
| Disconnected → Preparing | seule porte d'entrée d'une connexion |
| Preparing → AwaitingPermission / Connecting / ServerUnavailable / Error | vérif config + récupération de la session VPN |
| AwaitingPermission → Connecting / Disconnected | consentement `VpnService` accordé ou refusé |
| Connecting → Connected / Reconnecting / Error / ServerUnavailable | **« Connecté » seulement après établissement vérifié du tunnel** |
| Connected → Disconnecting / Reconnecting / Error | perte réseau → Reconnecting avec backoff exponentiel (1 s → 30 s max, 5 essais) |
| Error / ServerUnavailable → Disconnected / Preparing | toute erreur permet une sortie claire ou une nouvelle tentative |

Implémentation : `VpnStateMachine` (Kotlin pur, testé unitairement). Le libellé « **Protégé** »
n'est affiché que dans l'état `Connected` **et** après vérifications prévues en Phase 5
(handshake établi, DNS dans le tunnel). Jamais avant.

### Séquence de connexion (Phase 5)

```
UI ──onConnect──► DashboardVM ──► VpnRepository
   1. Vérif. connexion Internet + config locale (clé du device)
   2. POST /api/v1/vpn/session {server_id, pubkey_client}  → config éphémère
      (la clé privée ne quitte JAMAIS l'appareil)
   3. Si permission VpnService absente → AwaitingPermission → prepare()/onActivityResult
   4. startForeground(specialUse) → GoBackend.setState(UP, config)
   5. Vérif. handshake + interface → Connected (sinon backoff → Reconnecting)
```

### Séquence de déconnexion

Arrêt des reconnexions → `setState(DOWN)` → arrêt du service foreground → libération des
ressources → purge des données temporaires (config éphémère) → `Disconnected`.

Gestion des interruptions : Wi-Fi ↔ données mobiles, perte totale, redémarrage du processus
(`START_NOT_STICKY` + restauration d'état au boot de l'app, jamais de fausse reprise).

---

## 7. Sécurité et confidentialité (exigences techniques)

1. **Clés** : paire WireGuard générée sur l'appareil (`KeyPair` de la lib officielle) par appareil ;
   la clé privée est chiffrée via **Android Keystore** avant persistance. Jamais de clé partagée
   intégrée au code. Jamais de clé privée dans les logs/crash reports (interception des logs).
2. **Distribution de configuration** : le client n'envoie que sa clé **publique** ; le backend
   répond avec une config éphémère (adresse tunnel, DNS, endpoint, clé publique du serveur,
   AllowedIPs). Révocation = suppression du pair côté serveur.
3. **Anti-fuites** : DNS du tunnel uniquement, routes v4+v6, politique de reconnexion, recours au
   kill switch **système** ; vérification IP publique avant/après (Phase 5) ; tests de fuite DNS en Phase 6.
4. **Transport** : TLS 1.2+, validation de certificats standard (pas de TrustManager permissif) ;
   certificate pinning **optionnel** (coût opérationnel documenté, pas activé par défaut).
5. **Backend** : aucun panneau d'admin exposé sans authentification forte ; rate limiting ;
   pas de relais réseau arbitraire ; secrets uniquement en variables d'environnement serveur.
6. **Données** : minimisation (pas d'historique de navigation, jamais), journaux serveur minimaux,
   pas de SDK de tracking. **Pas de communication « zéro journal » tant que les pratiques
   opérationnelles ne le garantissent pas** (la politique de confidentialité sera écrite en Phase 4
   pour refléter la réalité).
7. **Chaîne de build** : scan des dépendances, détection de secrets avant tout commit public.

---

## 8. Backend & API (contrat — spécification complète en Phase 4)

Routes proposées (à affiner à l'implémentation, préfixées `/api/v1`, JSON, versionnées) :

| Route | Rôle | Auth |
|---|---|---|
| `GET /api/v1/health` | sonde de vie (monitoring) | publique |
| `GET /api/v1/servers` | catalogue + disponibilité + charge (si mesurée) | token device |
| `GET /api/v1/servers/{id}` | détail d'un serveur | token device |
| `POST /api/v1/vpn/session` | délivre une config tunnel éphémère pour un serveur | token device |
| `DELETE /api/v1/vpn/session/{id}` | clôture/révocation de session | token device |
| `GET /api/v1/account` · `GET /api/v1/subscription` | compte/offres (P1, optionnels) | token compte |

Erreurs normalisées : `{ "error": { "code": "SERVER_UNAVAILABLE", "message_key": "…", "detail": "…" } }`.
Limites de débit par jeton ; jetons de device révocables ; aucune donnée personnelle inutile.

---

## 9. Modèle de données (noyau)

- `Server { id, country, country_code, city, hostname, port, protocol, status, load?, tags[] }`
- `VpnSession { id, device_id, server_id, config_éphémère, créée, expire, révoquée }`
- `Device { id, token_révocable, clé_publique, créé }`
- `SessionStat (local uniquement) { serveur, début, fin, rx, tx }` — conservé seulement si l'utilisateur
  active l'historique local ; distinction claire avec toute statistique d'infrastructure.
- `AppSettings (DataStore)` : onboarding, thème, langue, reconnexion auto, dernier serveur, confirmation
  avant déconnexion, retour haptique, historique local.

---

## 10. Performance (budgets de conception)

| Objectif | Cible | Moyens |
|---|---|---|
| Démarrage à froid | < 2 s sur entrée de gamme | splash sans délai artificiel, pas d'I/O bloquant, `windowBackground` déjà à la bonne couleur |
| Fluidité | 60 fps quand l'appareil le permet | Compose, pas de sur-draw, animations courtes (180-280 ms), squelettes plutôt que spinners |
| Mémoire / batterie | minimales au repos | pas de service en arrière-plan hors tunnel actif ; statistiques à 1 Hz seulement pendant la session |
| Taille APK | < 15 Mo | pas de médias lourds, vectoriels, R8 activé |

Toute opération réseau est hors du thread principal (coroutines). Les animations ne conditionnent
jamais une opération réseau, et se désactivent via « Réduire les animations » + réglages système.

---

## 11. Limites du présent environnement de développement (vérifiées)

| Constat vérifié | Conséquence |
|---|---|
| Android SDK absent, Gradle absent, seul JDK 11 installé (AGP 8.x exige JDK 17+) | **Compilation impossible ici** ; le projet se construit dans Android Studio (JDK 17 fourni) |
| Le dossier `build/` n'est pas persisté dans l'espace de travail | Un APK compilé ici disparaîtrait ; donc aucun APK n'est livré à ce stade (Phase 7 : procédure de build + signature documentée) |
| Réseau ouvert (Maven Central et dl.google.com joignables) | Versions de dépendances vérifiées en ligne : `tunnel:1.0.20260102` confirmé |
| Pas d'appareil/émulateur Android ici | Les tests instrumentés et les tests de fuite se font sur un poste humain (Phase 6) ; tests unitaires purs fournis |
| Aucun serveur réel, aucun compte externe | Aucune connexion, IP, ping ou serveur inventé : le catalogue de démonstration est étiqueté `demo`, les mesures réelles renvoient `null` |

---

## 12. Ressources indispensables — action humaine requise

| # | Ressource | Pourquoi | Bloque |
|---|---|---|---|
| 1 | 1 à N VPS (ex. Ubuntu 24.04) avec IPv4 publique | Serveurs WireGuard réels | Phase 4/5 |
| 2 | Nom de domaine + DNS + certificats TLS (Let's Encrypt) | API backend fiable | Phase 4 |
| 3 | Petit hébergement backend (VPS ou PaaS) + sauvegardes | API serveurs/sessions | Phase 4 |
| 4 | Compte Google Play Console (25 $ US, uniq.) | Publication | Phase 7 |
| 5 | Appareils Android physiques (entrée de gamme si possible) | Tests réels, fuites, batterie | Phase 6 |
| 6 | Validation juridique (politique de confidentialité, CGU, RGPD/locales) | Conformité | Phase 7 |
| 7 | Compte paiement / Play Billing (si offre premium) | Monétisation | P1 ultérieur |

Tant que 1-3 ne sont pas fournis, l'application **ne peut pas** établir de tunnel réel — et l'affiche honnêtement.

---

## 13. Décisions prises par défaut (option la plus sûre et la plus simple)

1. **Userspace GoBackend** WireGuard (sans root) plutôt que module noyau.
2. **Sans compte obligatoire** au MVP : jeton de device anonyme révocable ; comptes plus tard si quotas/abonnements.
3. **Français** = ressources par défaut (`values/`), EN et PT en `values-en` / `values-pt`.
4. **Kill switch** délégué au système (fiable) plutôt qu'une réimplémentation fragile.
5. **Historique local désactivé par défaut**, effaçable en un geste.
6. **Pin TLS non activé** en v1 (coût d'exploitation), documenté et réversible.

---

## 14. Risques et mitigations

| Risque | Mitigation |
|---|---|
| Évolution de l'API/manifeste Android (FGS, permissions) | Veille à chaque montée de targetSdk ; ciblage 36 dès maintenant |
| Rejet Play (déclarations VPN) | Checklist dédiée Phase 7 : déclaration VPN, politique de confidentialité, `specialUse` justifié |
| Fuites DNS/IPv6 | Tests dédiés Phase 6 sur infrastructure de test ; sinon désactivation honnête des allégations |
| Abus du free tier | Quotas backend, jetons révocables, rate limiting |
| Dépendance WireGuard | Lib officielle Apache-2.0 sur Maven Central, version épinglée ; changelog suivi |

---

## 15. Plan de réalisation

Voir [`02_PLAN_REALISATION.md`](02_PLAN_REALISATION.md) — Phase 1 clôturée par ce document
et par le squelette Android exécutable (`android/`), la maquette identifiée (`demo/`) et l'amorce
de l'identité visuelle (`brand/`).
