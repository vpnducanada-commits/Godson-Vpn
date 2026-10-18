# GODSON VPN — Compilation, installation et tests (étapes exactes)

Honnêteté préalable : **aucun de ces tests n'a été exécuté dans cet environnement**
(pas de JDK 17 ni de SDK Android ici — constat vérifié en Phase 1). Les étapes sont
documentées pour être exécutées sur un poste humain ; les résultats attendus sont
indiqués, à confirmer à l'exécution.

---

## 1. Prérequis

| Outil | Version | Source |
|---|---|---|
| Android Studio | Ladybug (2024.2) ou plus récent | developer.android.com/studio |
| JDK | 17+ (embarqué avec Android Studio) | — |
| SDK Android | API 36 (installé automatiquement par Studio à la sync) | — |
| Appareil de test | Android 8.0+ (idéalement un téléphone physique entrée de gamme) | — |

Gradle 8.13 est téléchargé automatiquement via le wrapper (configuré dans
`gradle/wrapper/gradle-wrapper.properties`). Toutes les dépendances sont sur
Maven Central / Google Maven, y compris `com.wireguard.android:tunnel:1.0.20260102`.

## 2. Ouvrir et synchroniser le projet

1. Android Studio → **Open** → sélectionner le dossier `android/`.
2. Laisser la **sync Gradle** se terminer (première fois : quelques minutes).
3. Attendu : `BUILD SUCCESSFUL` / panneau projet rempli. En cas d'échec :
   vérifier connexion réseau (téléchargement des dépendances) puis
   File → Sync Project with Gradle Files.

## 3. Tests unitaires (JVM — aucun appareil requis)

```bash
cd android
./gradlew :app:testDebugUnitTest
```

- Suites : `VpnStateMachineTest` (7 tests), `ServerFilterTest` (7), `CountryCatalogTest` (10).
- Rapport HTML : `app/build/reports/tests/testDebugUnitTest/index.html`.
- Attendu : **24 tests, 0 échec**. Ces tests couvrent : machine à états
  (impossible d'afficher « Connecté » sans tunnel), filtres pays/serveurs,
  intégrité du catalogue (30 pays, drapeaux, « bientôt disponible » non connectable).

## 4. Compiler l'APK

```bash
./gradlew :app:assembleDebug
# Sortie : app/build/outputs/apk/debug/app-debug.apk
```

Variante release (signature : Phase 7 — keystore à créer par le porteur du projet) :

```bash
./gradlew :app:assembleRelease   # nécessite la config signingConfigs (Phase 7)
```

## 5. Installer et lancer sur un téléphone

```bash
# Activer « Options développeur » puis « Débogage USB » sur le téléphone,
# brancher en USB, accepter la clé de l'ordinateur, puis :
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s GodsonVpnService VpnController GodsonTunnel   # suivi en direct
```

## 6. Parcours de test (mode DÉMO — comportement ATTENDU)

1. Lancement : splash (~1,4 s) → onboarding 3 écrans → dashboard.
2. Badge **DÉMO** visible (catalogue fictif, mesures étiquetées).
3. « Choisir un pays » : 30 pays, recherche instantanée, filtres, favoris persistants.
4. Sélectionner **Gabon** (disponible en démo) → retour dashboard, drapeau affiché.
5. **Connecter** → Android affiche le **vrai dialogue de consentement VpnService** →
   Autoriser → après ~0,5 s : message d'erreur explicite « Tunnel non disponible »
   avec le corps « aucun backend n'est encore configuré… ».
   ✅ **C'est le résultat correct** : aucun faux « Connecté » ne doit apparaître.
6. Refuser le consentement → message « Autorisation VPN refusée » + Réessayer.
7. Tuer l'application → relance : pays sélectionné, favoris et thème conservés.
8. Thème clair/sombre, langues FR/EN/PT, « Réduire les animations » : effet immédiat.
9. Statistiques : lancer une « session » (en démo, seul l'état de l'app change) —
   vérifier qu'aucun volume de trafic n'est affiché (« Indisponible »).

## 7. Tests avec un backend réel (après docs/04_WIREGUARD_INFRA.md)

1. Modifier `API_BASE_URL` dans `app/build.gradle.kts` (debug + release) → rebuild.
2. Vérifier que les badges DÉMO ont disparu et que le catalogue affiche les
   serveurs réels (statuts du backend).
3. Connecter → consentement → attendre le handshake (~1-3 s).
4. **Vérifications de fuite** (à faire sur le téléphone connecté au tunnel) :
   - IP publique : mon-ip.com → doit afficher l'IP du VPS, pas la vôtre ;
   - DNS : dnsleaktest.com (test standard) → aucun résolveur du FAI ;
   - IPv6 : test-ipv6.com → pas d'adresse IPv6 native exposée si `::/0` routé.
5. Couper le Wi-Fi pendant une session → état Reconnecting puis sortie propre.
6. Activer « VPN permanent + Bloquer sans VPN » dans les réglages Android →
   couper le tunnel → vérifier qu'aucun trafic ne passe hors tunnel.
7. `adb logcat` : vérifier qu'aucune clé privée ni donnée personnelle n'apparaît.

## 8. Ce qui n'est PAS encore testé (liste honnête)

- Compilation Gradle complète (environnement actuel sans SDK) — erreurs possibles
  sur la surface d'API de la lib WireGuard (`VpnController`) ; correctifs attendus
  à la première compilation.
- Tests instrumentés et sur appareil (Phase 6).
- Cycle tunnel réel complet (exige l'infrastructure de docs/04).
- Fuites DNS/IPv6 (Phase 6, environnement de test contrôlé).
