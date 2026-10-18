# 08 — Publication : signature release, secrets CI, Play Store

> **Statut** : chaîne de construction et de signature **opérationnelle dans la CI**.
> Tout est réalisable **depuis un téléphone** (Termux pour le keystore, github.com pour les
> secrets et la console Play). Aucun ordinateur requis.

## 1. Créer le keystore de publication (une seule fois, ~5 min)

Sur **Termux** (openjdk-17 déjà installé au préalable, voir docs/07) :

```bash
keytool -genkeypair -v \
  -keystore ~/godson-release.keystore \
  -alias godson \
  -keyalg RSA -keysize 4096 -validity 10000
```

Répondre aux questions (nom/organisation — aucune donnée sensible obligatoire) et choisir
**deux mots de passe** (keystore + clé ; ils peuvent être identiques).

⚠️ **Règles absolues**
- Ce fichier EST l'identité de l'application : **sauvegarde-le** (cloud personnel + autre support).
- Ne le commite **jamais** dans le dépôt, ne l'envoie **jamais** par canal non chiffré.
- S'il est perdu, l'application ne peut plus être mise à jour sur Play (nouvelle identité).

## 2. Ajouter les 4 secrets GitHub (depuis le navigateur du téléphone)

1. Encoder le keystore en base64 (Termux) : `base64 -w0 ~/godson-release.keystore`
   → copier **toute** la chaîne.
2. github.com → dépôt **Godson-Vpn** → **Settings** → **Secrets and variables → Actions** →
   **New repository secret**, quatre fois :

| Nom du secret | Valeur |
|---|---|
| `KEYSTORE_BASE64` | la chaîne base64 de l'étape 1 |
| `KEYSTORE_PASSWORD` | mot de passe du keystore |
| `KEY_ALIAS` | `godson` |
| `KEY_PASSWORD` | mot de passe de la clé |

Les secrets sont masqués dans les logs de CI et ne sont jamais affichés.

## 3. Produire l'APK/AAB signé

GitHub → onglet **Actions** → workflow **« Build Android (tests + APK debug) »** →
bouton **Run workflow** (branche `main`) → le job **release** construit :

- artefact **`app-release-apk`** : APK signé (installation directe, distribution hors Play) ;
- artefact **`app-release-aab`** : Android App Bundle (exigé par Google Play).

En parallèle, le job **build** continue de produire l'APK debug de test à chaque push.
Repli documenté : sans secrets, un build `release` local/CI est signé avec la clé **debug**
(uniquement pour essai sideload — **jamais** pour Play).

## 4. Checklist Google Play (compte développeur requis : 25 $ USD, une fois)

| Élément | État |
|---|---|
| SDK cible 36 (obligatoire depuis 31/08/2026) | ✅ fait |
| AAB signé (upload key) | ✅ job release |
| **Déclaration VPN** : l'app utilise `VpnService` → case « VPN app » dans la déclaration, justificatif du `foregroundServiceType="specialUse"` prêt dans le manifeste | ✅ à copier-coller |
| **Data Safety** : aucune donnée collectée/transmise obligatoire ; identifiant d'appareil **local** jamais transmis ; pas de publicité ; pas de revente | ✅ formulable tel quel |
| **Politique de confidentialité** : URL publique exigée | ⛔ bloqué : nécessite un hébergement (Phase 4) — la version provisoire existe dans l'app |
| Classification du contenu, catégorie, coordonnées | 🔲 à remplir sur la console |
| Piste de test : **Internal testing** d'abord (jusqu'à 100 testeurs), puis closed/open | 🔲 recommandé avant production |

## 5. Notes de version (prêtes à coller)

- **FR** : « Première version d'essai : interface complète (mode démonstration clairement
  étiqueté), catalogue de 30 destinations, GODSON PULSE (évaluation de qualité), tunnelage
  fractionné, français/anglais/portugais natifs. Aucun tunnel réel tant que l'infrastructure
  serveur n'est pas active — l'application l'affiche honnêtement. »
- **EN** : « First trial release: complete interface (clearly labeled demo mode), 30-destination
  catalog, GODSON PULSE quality assessment, split tunneling, native French/English/Portuguese.
  No real tunnel until the server infrastructure is live — the app says so honestly. »
- **PT** : « Primeira versão de teste: interface completa (modo demonstração claramente
  rotulado), catálogo de 30 destinos, avaliação de qualidade GODSON PULSE, tunelamento
  dividido, francês/inglês/português nativos. Nenhum túnel real até que a infraestrutura de
  servidores esteja ativa — o aplicativo informa isso com honestidade. »

## 6. Guide de reprise développeur

- **Construire** : `./gradlew :app:assembleDebug` (essai) / job CI `release` (publication).
- **Tester** : `./gradlew :app:testDebugUnitTest` (61 tests, JUnit4).
- **Passer en mode réel** : remplacer les `API_BASE_URL` `.invalid` dans
  `android/app/build.gradle.kts` (debug + release) par l'URL du backend réel (Phase 4) —
  l'application bascule seule (voir `data/AppMode.kt`).
- **Structure** : docs/01 architecture · docs/02 plan · docs/03 design · docs/04 infra
  WireGuard · docs/05 build local · docs/06 audit + PULSE · docs/07 Termux · docs/08 (ce document).

## 7. Blocages restants (honnêteté)

| Blocage | Se débloque par |
|---|---|
| Compte Google Play (25 $) + pièce d'identité | Porteur du projet |
| URL de politique de confidentialité publique | Hébergement statique (Phase 4, ou une GitHub Page) |
| Vrais serveurs (Phase 4/5) | VPS + domaine (docs/04) |
