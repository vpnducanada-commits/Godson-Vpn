# GODSON VPN — Plan de réalisation (7 phases)

Ce fichier est mis à jour à chaque phase : statut, livrables réels, blocages, prochaines actions.
Règle : ne jamais marquer « terminé » sans vérification.

---

## PHASE 1 — Analyse et architecture — ✅ TERMINÉE

| Livrable | Statut |
|---|---|
| Architecture, contraintes Android, choix techno justifiés | ✅ `docs/01_PHASE1_ARCHITECTURE.md` |
| Vérification de l'environnement (SDK/JDK/réseau) | ✅ Constats consignés (§11 de l'architecture) |
| Versions critiques vérifiées (WireGuard `tunnel:1.0.20260102`, FGS `specialUse`, Play targetSdk 36) | ✅ |
| Squelette Android exécutable : Gradle (catalogue de versions), manifest VPN (`specialUse`), thème/palette officielle, navigation Compose complète (splash → onboarding → 4 onglets), machine à états VPN + test unitaire, service VPN squelette avec points d'intégration Phase 5, DataStore (préférences), catalogue de démonstration étiqueté | ✅ `android/` |
| Maquette interactive clairement identifiée (design de référence) | ✅ `demo/godson_vpn_demo.html` |
| Identité visuelle : propositions de logo GV (vote) | 🟡 `brand/` |

**Blocages** : compilation et tests instrumentés impossibles ici (pas de JDK 17/SDK — voir §11 de l'architecture).
**Prochaine action** : Phase 2.

---

## PHASE 2 — Identité visuelle et interface — ✅ TERMINÉE (refonte premium v2 appliquée)

- [x] Palette officielle implémentée (`ui/theme/`) + dégradés/halos maîtrisés (`Color.kt`)
- [x] Logo GV vectoriel officiel : `brand/*.svg` (4 variantes) + `ic_logo_gv.xml` + icône adaptative
- [x] Design system documenté : `docs/03_DESIGN_SYSTEM.md` + composants réutilisables (`ui/components/GodsonComponents.kt`)
- [x] **Dashboard premium réel** : en-tête de marque, bouton circulaire à anneaux concentriques animés
      (4 états visuels), statut de sécurité réel, carte serveur (badge drapeau, signal, disponibilité),
      indicateurs compacts, **vrai dialogue de consentement VpnService**, erreur honnête NO_BACKEND
- [x] **Écran « Emplacements »** : recherche instantanée, catégories, tri par latence, barres de signal,
      favoris **persistants** (DataStore), récents persistants, skeletons, états vides illustrés
- [x] Écrans secondaires : Statistiques (durées réelles + chronologie), Paramètres (interrupteurs
      fonctionnels persistés), Confidentialité (état réel + réglages système), Assistance
      (FAQ + diagnostic réel + presse-papiers), Profil (mode sans compte + identifiant local)
- [x] Micro-interactions : transitions 240 ms, cascade des cartes, pressScale, anneaux animés,
      respect de « Réduire les animations » et `prefers-reduced-motion`
- [x] Maquette de référence mise à niveau en miroir (`demo/godson_vpn_demo.html`), FR/EN/PT,
      persistance navigateur (favoris, thème, langue, identifiant), jamais de faux « Protégé »
- [x] Contrôles statiques exécutés : JS/XML OK, ressources référencées complètes, Kotlin équilibré,
      API repositories cohérentes, 9 routes de navigation — **voir rapport QC dans la conversation**
- [x] **Catalogue de pays (30 pays / 6 régions)** : géographie séparée de l'état serveur
      (`Country.kt` × `ServerInfo.status`), écran « Choisir un pays » (recherche instantanée,
      filtres continent + disponibilité, tris A–Z/latence, favoris et récents persistants),
      « Bientôt disponible » non connectable pour les pays sans serveur, sélection persistée
      (DataStore) et consommée par le bouton Connecter du dashboard. Tests : `CountryCatalogTest`

---

## PHASE 3 — Fonctionnement applicatif — 🔲

- [ ] ViewModels complets (dashboard, serveurs, stats, paramètres)
- [ ] Favoris, récents, tri/filtres, connexion rapide
- [ ] Statistiques de session réelles (compteurs du tunnel disponibles en Phase 5 ; UI prête)
- [ ] Gestion des erreurs/états vides/hors ligne (10 cas listés au cahier des charges)
- [ ] Localisation FR/EN/PT complète, accessibilité (tailles système, animations réduites)
- [ ] Modularisation si nécessaire

---

## PHASE 4 — Infrastructure réelle — 🔲 *(exige action humaine : VPS, domaine, hébergement)*

- [ ] Backend FastAPI : `/api/v1/health`, `/servers`, `/vpn/session` (+ révocation), auth par jeton de device
- [ ] Modèle de données + migrations ; environnements dev/staging/prod
- [ ] Guide de déploiement backend + guide serveurs WireGuard (par serveur : identité propre, clés, monitoring)
- [ ] Branchement du client Android sur l'API + mesure réelle de latence
- [ ] Politique de confidentialité reflétant la réalité opérationnelle

---

## PHASE 5 — Intégration VPN réelle — 🔲 *(exige Phase 4)*

- [ ] GoBackend intégré à `GodsonVpnService` (cycle complet, FGS `specialUse`)
- [ ] Clés par appareil, stockage chiffré Keystore, config éphémère
- [ ] Reconnexion automatique avec backoff, gestion des changements de réseau
- [ ] Anti-fuites : DNS tunnel, routes v4/v6, intégration Always-on VPN/kill switch système
- [ ] Vérification IP publique avant/après ; libellé « Protégé » conditionné aux vérifications

---

## PHASE 6 — Tests et sécurité — 🔲 *(tests instrumentés sur poste humain/appareils)*

- [ ] Tests unitaires (machine à états, repositories, parsing API) — exécution Gradle
- [ ] Tests UI Compose + Parcours (premier lancement, refus permission, perte réseau…)
- [ ] Tests de fuite IP/DNS/IPv6 sur infrastructure de test contrôlée
- [ ] Scan dépendances + détection de secrets + revue des logs (aucune donnée sensible)
- [ ] Rapport de tests honnête (exécutés / résultats / restants)

---

## PHASE 7 — Préparation à la publication — 🟡 CHAÎNE OPÉRATIONNELLE (compte Play et hébergement restants)

- [x] **Génération APK/AAB automatisée** : job CI `release` (déclenchement manuel) construisant
      APK + AAB **signés** à partir des secrets du dépôt — procédure complète sans ordinateur
      dans `docs/08_PUBLICATION_PLAY.md`
- [x] **Signature release** : `signingConfigs` pilotée par l'environnement (secrets CI) ; repli
      documenté clé debug (essai sideload uniquement, jamais Play)
- [x] **Version** : `0.3.0-phase3` (code 3) — phases 1 à 3 livrées
- [x] **Checklist Play** : déclarations VPN/specialUse prêtes, Data Safety formulable (aucune
      donnée collectée), notes de version FR/EN/PT rédigées, guide de reprise développeur
- [ ] **Bloqué porteur** : compte Google Play (25 $), URL publique de politique de confidentialité
- [ ] **Bloqué infra** : vrais serveurs (Phase 4/5) avant piste de production

---

## PHASE 4/5 — Backend RÉEL prêt à déployer — 🟢 CODE COMPLET ET TESTÉ (déploiement = 1 commande)

- [x] **backend/server.py** : API v1 exacte de l'app (health, servers, vpn/session,
      DELETE) en Python standard (0 dépendance) ; une session = un pair WireGuard
      (/32 dédié, révoqué au DELETE) ; aucune clé dans les journaux ; erreurs
      {"error":{code,message}} conformes
- [x] **Testé ici-même** (mode dry-run WireGuard) : cycle complet créer→supprimer→404,
      contrats JSON **programmatiquement conformes aux DTO Kotlin**, correction d'un bug
      DELETE attrapé par le test (chemin à 5 segments)
- [x] **backend/install.sh** : installation VPS en UNE commande (WireGuard + clés serveur
      + NAT + systemd + Caddy HTTPS Let's Encrypt + pare-feu + auto-vérification) —
      guide téléphone complet dans **docs/09_BACKEND_SERVEUR_REEL.md** (DuckDNS gratuit)
- [ ] **Reste (porteur)** : louer le VPS (ou Oracle Free), créer le sous-domaine DuckDNS,
      coller la commande du docs/09 §3, puis donner l'URL → l'APK est reconstruit en mode
      RÉEL par la CI

## PHASE 4/5 (historique) — Architecture backend + WireGuard — 🟡 ARCHITECTURE EN PLACE (non testée sur appareil)

Préparé (code complet, câblé, vérifié statiquement — exécution sur appareil restante) :

- [x] **Séparation démo/réel automatique** : `data/AppMode.kt` (URL `.invalid` ⇒ DÉMO obligatoire) ;
      câblé dans Application (init GoBackend), Container, DashboardViewModel, GodsonVpnService, ServerRepository
- [x] **Couche API v1** : `api/GodsonApi.kt` (health, servers, vpn/session, close session),
      `api/ApiModels.kt` (DTOs sérialisables), `api/ApiClient.kt` (Retrofit+OkHttp, TLS standard, pas de secret)
- [x] **Couche WireGuard réelle** : `vpn/VpnKeyManager.kt` (paire par appareil, clé privée jamais transmise),
      `vpn/TunnelConfigMapper.kt` (Config/Interface/Peer de la lib officielle, DNS dans le tunnel, 0.0.0.0/0+::/0),
      `vpn/VpnController.kt` (cycle complet : session → config → GoBackend setState → **vérification isTunnelAlive**
      → Connected uniquement après preuve ; stats tunnel réelles à 1 Hz ; détection de mort du tunnel)
- [x] **Serveurs réels** : `ServerRepository` branche GET /api/v1/servers en mode réel (statuts tolérants, drapeaux ISO)
- [x] **Erreurs** : NO_BACKEND (démo), PERMISSION_DENIED, CONNECTION_FAILED — messages explicites + Réessayer
- [x] **Docs** : `docs/04_WIREGUARD_INFRA.md` (VPS/domaine/clés exacts, contrat API, checklist sécurité),
      `docs/05_BUILD_AND_TEST.md` (compile, install, parcours de test, tests de fuite)
- [ ] À faire sur poste humain : première compilation Gradle (surface d'API WireGuard à confirmer),
      tests instrumentés, backend réel (Phase 4), tests de fuite (Phase 6) — **rien de tout cela n'a été exécuté ici**

---

## GODSON PULSE — moteur de qualité + intégration UI — ✅ NOYAU ET UI EN PLACE (non compilés ici)

- [x] **Noyau (Étape 2)** : `pulse/PulseProber.kt` (modèle validé à la construction + contrat
      de sonde), `pulse/DemoProber.kt` (déterministe, étiqueté DEMO, refuse les cibles non démo),
      `pulse/PulseScore.kt` (score 0–100 : 0,60 latence + 0,40 stabilité, seuils 300 ms/30 %/80 ms,
      niveaux ≥85/≥65/≥45), `pulse/PulseEngine.kt` (états OK / DEMO_ONLY / UNAVAILABLE, classement
      déterministe, plafond 10) — spec : `docs/06_GODSON_PULSE_SPEC.md`
- [x] **Tests écrits** : 26 tests unitaires (13 `PulseScoreTest` + 13 `PulseEngineTest`) —
      **0 exécuté ici** (pas de JDK 17/Gradle ; vérifications statiques + recalcul indépendant OK)
- [x] **Intégration UI discrète** (écran Emplacements) : pastille « score · niveau » colorée par
      serveur évalué + ligne de synthèse étiquetée « Qualité réseau (démo) », alimentées par
      `PulseEngine.evaluate()` via `ServersViewModel.evaluatePulse()` — **une passe à la demande**
      (ouverture de l'écran ou bouton Rafraîchir, aucune boucle de fond) ; serveurs hors service
      jamais évalués ni recommandés ; injection de la sonde dans `GodsonContainer.pulseProber` ;
      clés `pulse_*` ajoutées à `strings.xml` (7)
- [x] **Intégration tableau de bord** : carte « GODSON PULSE » (badge démo) sous le serveur
      sélectionné — recommandation avec point coloré + « Utiliser ce serveur » (sélection persistée
      via `DashboardViewModel.applyPulseRecommendation()`, masqué si déjà sélectionné) ; helpers
      partagés `pulseLevelColor`/`qualityLabelRes` dans `GodsonComponents` (les copies privées de
      `ServersScreen` seront unifiées à la passe de compilation CI) ; clé `pulse_action_use` ajoutée
- [x] **Comportement mode réel (aujourd'hui)** : sans sonde réelle, les cibles réelles sont
      refusées par DemoProber ⇒ rapport UNAVAILABLE ⇒ ligne « évaluation indisponible »,
      aucune valeur inventée
- [x] **CI GitHub Actions prête** : `.github/workflows/build.yml` créé (tests `:app:testDebugUnitTest`
      + APK debug publiés en artefact téléchargeable ; déclenchement au push ou manuel).
      Validation locale : code compilé (compileDebugKotlin ✓) et tests passés — 51/51 puis
      **61/61** (10 tests de tunnelage fractionné ajoutés) — dans le
      bac à sable (JDK 17 + SDK 36 + Gradle 8.13 réels) ; seul le packaging final APK
      (mergeExtDexDebug) dépasse la RAM du bac à sable — les serveurs GitHub (7 Go) la font en
      quelques minutes. **Validé en conditions réelles : run 39105516644 — BUILD SUCCESSFUL (tests 51/51 + APK 31 Mo), artefact récupéré.**
- [ ] **Bloqué** : sonde réelle `HttpProber` (3 échantillons TCP, médiane) — requiert le backend
      Phase 4 ; à ce moment, injecter la sonde dans le conteneur suffit, l'UI ne change pas

---

## TUNNELAGE FRACTIONNÉ (split tunneling) — ✅ IMPLÉMENTÉ (validation CI)

- [x] **Logique pure testée** : `data/SplitTunnelPolicy.kt` — modes BLOCK (tout sauf les apps
      choisies) / ALLOW (seulement les apps choisies) ; garde-fous : l'app GODSON jamais dans
      le tunnel, avertissement explicite « ALLOW vide », filtre/tri déterministes —
      **+10 tests** (total projet : 61)
- [x] **Persistance DataStore** : `split_tunneling_enabled/mode/apps` (SettingsRepository)
- [x] **Câblage réel** : `TunnelConfigMapper.build(+excludedApps, +includedApps)` via
      `excludeApplication/includeApplication` — appliqués par GoBackend sur le VpnService.Builder
      (**vérifié dans son bytecode**) ← `VpnController.connectReal` ← lecture à la demande dans
      `DashboardViewModel.establishTunnel` (jamais en boucle de fond)
- [x] **UI** : rangée Réglages (compteur d'apps) + écran `SplitTunnelScreen` (inventaire
      PackageManager RÉEL — aucune liste fictive, recherche, sélection, avertissement) +
      route `split_tunneling`
- [ ] **Mode démo** : réglages persistés mais sans effet tant qu'aucun tunnel réel n'existe
      (mention affichée dans l'écran)

---

## PHASE 3 — Fonctionnement applicatif — ✅ LIVRÉ (polices Inter, EN/PT natifs, split tunneling)

- [x] **Split tunneling** : voir section dédiée ci-dessus (logique testée, DataStore, UI,
      câblage GoBackend vérifié au bytecode) — 10 tests
- [x] **Police Inter embarquée** (OFL, release officielle rsms v4.1) : 4 graisses dans
      `res/font/`, `InterFamily` branchée sur les 10 styles (tailles en sp — respect de la
      police système de l'utilisateur conservée)
- [x] **i18n natif EN/PT** : `values-en/` + `values-pt/` (220 clés chacune, parité et
      placeholders vérifiées automatiquement) + `resourceConfigurations(fr,en,pt)` +
      `locales_config.xml` (langue par application, Android 13+) — français reste la langue
      par défaut
- [x] **Garde-fou exécution** : l'app elle-même est exclue du tunnel via le nom de paquet
      RÉEL (identique en release, suffixé `.debug` en debug)

---

## Registre des blocages

| Blocage | Contournement | Se débloque par |
|---|---|---|
| Pas de JDK 17/Android SDK ici | Code source complet + instructions de build | Build dans Android Studio (humain) |
| `build/` non persisté | Aucun artefact binaire livré ici | Idem |
| Aucune infra serveur | Catalogue de démo étiqueté, API simulée proprement | Fournir VPS + domaine (Phase 4) |
| Pas d'appareil Android | Maquette + tests unitaires purs | Tests sur appareils (Phase 6) |
