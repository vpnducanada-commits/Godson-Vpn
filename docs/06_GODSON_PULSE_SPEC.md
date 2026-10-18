# GODSON PULSE — Spécification technique v1.0

| | |
|---|---|
| **Statut** | Noyau implémenté (pur Kotlin), **non compilé** (voir limites §8) |
| **Principe** | Évaluer la qualité du réseau et recommander un serveur — **sans jamais inventer de mesure** |
| **Références** | `docs/06_AUDIT_TECHNIQUE.md` (audit du 18/10/2026), `docs/01_PHASE1_ARCHITECTURE.md` |

---

## 1. Rôle et non-rôles

**GODSON PULSE évalue** : latence, gigue (stabilité), taux d'échec des échantillons,
joignabilité d'une cible — puis recommande le serveur le mieux classé.

**GODSON PULSE ne fait PAS** :
- ❌ établir ou modifier le tunnel VPN (aucun contact avec `VpnService`/WireGuard) ;
- ❌ toucher aux clés ou au trafic utilisateur ;
- ❌ fonctionner en boucle ou en arrière-plan (mesure uniquement **à la demande**) ;
- ❌ prétendre qu'un VPN améliore la vitesse Internet ;
- ❌ considérer une réponse HTTP/TCP comme la preuve qu'un serveur VPN est opérationnel
  (elle prouve seulement que la cible réseau répond).

## 2. Architecture (couche `com.godson.vpn.pulse`)

```
┌──────────────────────────────────────────────────────┐
│ UI (future intégration, badge DEMO obligatoire)      │
└──────────────▲───────────────────────────────────────┘
               │ PulseReport
┌──────────────┴───────────────┐
│ PulseEngine                  │  orchestration à la demande,
│  - probeAndEvaluate(...)     │  classement déterministe,
│  - evaluate(results, now)    │  états explicites
└──────┬───────────────────────┘
       │ PulseProber (interface)
┌──────┴───────────────┐   ┌───────────────────────┐
│ HttpProber (futur)   │   │ DemoProber (présent)  │
│ mesures TCP réelles  │   │ valeurs déterministes │
│ source = REAL        │   │ source = DEMO         │
└──────────────────────┘   └───────────────────────┘
       PulseTarget · PulseResult · PulseScore · QualityLevel
```

Le **noyau est pur** (aucune I/O dans `evaluate`) : testable sans appareil.
Les sondes sont interchangeables (interface `PulseProber`).

## 3. Modèle de données

| Type | Rôle | Points clés |
|---|---|---|
| `PulseSource` | `REAL` / `DEMO` | Porté par chaque mesure ; comparaison uniquement à source égale |
| `PulseTarget` | Cible de sonde | `isRealProbeable` : refuse `.invalid`, `.test`, `.localhost`, `.example` (RFC 2606/6761) comme cibles réelles ; port 1..65535 validé |
| `ProbeResult` | Mesure normalisée | latence/jigue/perte bornées (validation à la construction, `IllegalArgumentException` sinon) ; `isCoherent` : cible injoignable ⇔ pas de latence ; `hasValidLatency` |
| `QualityLevel` | 4 niveaux documentés | EXCELLENT ≥ 85 · GOOD ≥ 65 · FAIR ≥ 45 · POOR < 45 |
| `PulseRecommendation` | Meilleur serveur | Toujours accompagné de sa source — une reco DEMO ne peut pas se faire passer pour réelle |
| `PulseStatus` | État du rapport | `OK` (réel) · `DEMO_ONLY` (données simulées étiquetées) · `UNAVAILABLE` (aucune donnée valide) |
| `PulseReport` | Résultat complet | statut, reco, classement (≤ 10), compteurs real/demo, horodatage |

## 4. Algorithme de score (déterministe, version 1.0)

### Constantes officielles (documentées dans `PulseScore`)

| Constante | Valeur | Signification |
|---|---|---|
| `LATENCY_MAX_MS` | 300 | latence ≥ 300 ms ⇒ composante latence = 0 |
| `JITTER_MAX_MS` | 80 | gigue ≥ 80 ms ⇒ composante gigue = 0 |
| `LOSS_MAX` | 0,30 | perte ≥ 30 % ⇒ composante perte = 0 |
| `WEIGHT_LATENCY` | 0,60 | poids global latence |
| `WEIGHT_STABILITY` | 0,40 | poids global stabilité (perte 0,60 / gigue 0,40 en interne) |

### Formules

```
latencyScore = 100 × (1 − min(latence / 300, 1))
lossScore    = 100 × (1 − min(perte / 0,30, 1))
jitterScore  = 100 × (1 − min(gigue / 80, 1))

score = (0,60 × latencyScore + 0,40 × (0,60 × lossScore + 0,40 × jitterScore))
        ──────────────────────────── renormalisé ────────────────────────────
        diviseur = 0,60 + 0,40 × (somme des poids de stabilité disponibles)
```

**Données manquantes** : chaque composante absente est exclue, les poids sont
renormalisés — jamais de pénalité ni de bonus inventé. Une mesure ne portant que
la latence produit `score = latencyScore`.

**Exemple chiffré** (latence 20 ms, perte 0, gigue 5 ms) :
latencyScore = 93,33 · lossScore = 100 · jitterScore = 93,75
stabilité = 0,6×100 + 0,4×93,75 = 97,5 → score = 0,6×93,33 + 0,4×97,5 = **95** (EXCELLENT).

**Classement** : score décroissant → latence croissante → identifiant alphabétique.
Toujours déterministe, plafonné à 10 entrées exposées.

## 5. Sonde de démonstration (`DemoProber`)

- Aucune I/O, aucun aléatoire : valeurs **déterministes** dérivées de l'identifiant
  serveur (`stableSeed`, somme pondérée des caractères) → latence 15–214 ms,
  gigue 2–21 ms, perte 0 ou 2 %.
- Chaque résultat porte `source = DEMO` et `detail = "Valeur de démonstration…"`.
- Une cible non marquée `isDemoTarget` est **refusée** (échec explicite).
- Ne prouve jamais qu'un serveur est opérationnel.

## 6. Sonde réelle future (`HttpProber` — plan d'intégration Phase suivante)

- 3 échantillons d'ouverture TCP vers `target.host:port`, à la demande ;
- `latencyMs` = médiane des échantillons réussis ; `jitterMs` = moyenne absolue des
  écarts entre échantillons successifs ; `lossRate` = échecs / total ;
- délai d'attente : ≤ 2 s par échantillon (`PulseProber.PROBE_TIMEOUT_MS`) ;
- uniquement des cibles `isRealProbeable` (jamais `.invalid`) ;
- échec/exception → `ProbeResult.unavailable` (jamais d'exception propagée) ;
- **blocage documenté** : sans backend réel, aucune cible réelle existe → la sonde
  réelle ne sera activée qu'avec l'infrastructure Phase 4 (le moteur la classera
  automatiquement et écartera les données DEMO).

## 7. Cas d'erreur (comportements testés)

| Cas | Comportement |
|---|---|
| Aucun résultat | `UNAVAILABLE`, reco `null` |
| Toutes les sondes échouent | `UNAVAILABLE`, reco `null` |
| Mesures incohérentes (joignable sans latence, etc.) | Exclues du classement, jamais interprétées |
| Valeurs hors bornes (perte > 1, latence < 0 ou > 5 000) | Construction refusée (`IllegalArgumentException`) |
| Mélange REAL + DEMO | Le réel prime ; la démo est écartée du classement |
| DEMO uniquement | `DEMO_ONLY` + reco explicitement `source = DEMO` |
| Exception d'une sonde | Convertie en échec explicite (détail = type d'exception, sans donnée personnelle) |

## 8. Limites connues (honnêteté)

1. **Compilation non effectuée** : environnement Arena AI sans JDK 17/SDK (audit §D).
   Le code respecte les conventions du projet mais la surface réelle du compilateur
   Kotlin n'a pas tranché.
2. **Tests non exécutés** : 26 tests écrits (13 `PulseScoreTest` + 13 `PulseEngineTest`) ;
   exécution prévue via `./gradlew :app:testDebugUnitTest` — aucun succès revendiqué.
3. La sonde `probeAndEvaluate` (suspend) n'est pas couverte par les tests (exigerait
   `kotlinx-coroutines-test`, dépendance évitée volontairement) ; seule la voie pure
   `evaluate()` est testée.
4. Une latence TCP mesure le chemin réseau vers une cible — **pas** la performance
   future d'un tunnel VPN sur ce chemin.
5. Les seuils (300/80 ms, 30 %) sont des choix produits raisonnables, pas des normes ;
   ils sont constants, documentés et centralisés pour être ajustés en connaissance de cause.

## 9. Règles d'éthique des mesures (résumé contractuel)

1. Toute mesure porte sa source ; l'UI doit afficher le badge DÉMO le cas échéant.
2. Pas de score affiché sans mesure valide (`UNAVAILABLE` explicite).
3. Mesures à la demande uniquement ; pas de boucle ; pas de service en arrière-plan.
4. Aucune promesse d'accélération Internet.
5. Aucune donnée personnelle dans les résultats (`detail` = technique et anonyme).

## 10. Fichiers de la couche PULSE

| Fichier | Rôle |
|---|---|
| `pulse/PulseProber.kt` | Modèle (`PulseTarget`, `ProbeResult`, `PulseSource`) + contrat de sonde |
| `pulse/DemoProber.kt` | Sonne de démonstration déterministe, étiquetée DEMO |
| `pulse/PulseScore.kt` | Score 0–100 déterministe + niveaux et seuils documentés |
| `pulse/PulseEngine.kt` | Orchestration à la demande + classement + recommandation + états explicites |
| `test/.../PulseScoreTest.kt` | 11 tests du score (nominal, bornes, manquant, invalide, contradictoire) |
| `test/.../PulseEngineTest.kt` | 10 tests du moteur (classement, sources, indisponibles, DEMO, échec total, déterminisme) |
