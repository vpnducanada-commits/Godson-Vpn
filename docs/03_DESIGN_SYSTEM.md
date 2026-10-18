# GODSON VPN — Design System v2 (« Premium Cybersecurity — Futuristic Minimalism »)

Référence unique pour tous les écrans (Android Compose + maquette `demo/`).

## 1. Couleurs

| Jeton | Valeur | Usage |
|---|---|---|
| `MidnightNavy` | #07111F | Fond principal |
| `DeepNavy` | #0B1730 | Surfaces (cartes) |
| `PanelNavy` | #0F1D3C | Surfaces surélevées |
| `ElectricBlue` | #246BFD | Actions principales |
| `BlueDeep` | #1A56D6 | Extrémité des dégradés d'action |
| `CyberCyan` | #21D4FD | Accents, indicateurs, sélection |
| `SoftWhite` | #F5F8FF | Texte principal (thème sombre) |
| `SlateGray` | #91A0B8 | Texte secondaire |
| `SuccessGreen` | #25D695 | **Uniquement** état connecté vérifié |
| `WarningAmber` | #FFB547 | Badge « DÉMO », avertissements |
| `ErrorRed` | #FF5364 | Erreurs, favori actif |
| `GlowCyan`/`GlowGreen` | alpha 0x22-0x33 | Halos très subtils (jamais de néon) |

Sémantique stricte : vert = vérifié, rouge = erreur, bleu = action/info, ambre = prudence.

## 2. Typographie (échelle)

Sans-serif système en Phase 2 (Inter/Manrope en Phase 3), tailles sp (respect réglages système) :

| Style | Poids / taille / interligne |
|---|---|
| headlineSmall | 700 / 22 / 28 — titres d'écran |
| titleLarge | 600 / 19 / 26 — valeurs fortes |
| titleMedium | 600 / 16 / 22 — titres de cartes |
| bodyLarge | 400 / 16 / 24 — texte principal |
| bodyMedium | 400 / 14 / 20 — descriptions |
| bodySmall | 400 / 12 / 16 — notes |
| labelLarge | 500 / 14 — boutons |
| labelMedium | 500 / 12 — étiquettes, onglets |

## 3. Formes et espacements

- Rayons : 8 (extra-small) / 10 (chips) / 16 (cartes compactes, champs) / 20 (cartes) / 999 (pillules).
- Contours : 1 dp `outlineVariant` (SlateGray α 0,18 sombre / 0,3 clair). Jamais double bord.
- Marges d'écran : 20 dp horizontal. Espacement entre cartes : 10-14 dp. Interligne de section : 18 dp au-dessus.
- Zones tactiles : ≥ 48 dp. Boutons pleine largeur : 52-54 dp de haut.

## 4. Élévation et profondeur

- Pas d'ombres portées ; profondeur = différenciation de surface + contour + halo radial maîtrisé.
- Halos : `Brush.radialGradient` alpha ≤ 0,20, jamais animés en permanence (respiration uniquement à l'état connecté, 1,6 s).

## 5. Mouvement

| Usage | Spécification |
|---|---|
| Transition d'écran | fondu 240 ms + translation verticale h/24 ; sortie 180 ms |
| Apparition des cartes de liste | cascade 35 ms/élément (plafond 8 éléments), 240 ms |
| Pression | scale 0,96-0,98, 130 ms |
| Anneau de connexion | rotation 1,5 s linéaire (état occupé uniquement) |
| Changement de couleur | 220-300 ms |
| Réduction | `LocalReducedMotion` → durées 0-90 ms, rotation supprimée |

Règle absolue : aucune animation ne simule une réussite réseau. Le statut suit la machine à états.

## 6. Composants (source : `ui/components/GodsonComponents.kt`)

`GodsonCard`, `SubHeader`, `SectionLabel`, `DemoBadge`, `FlagBadge`, `SignalBars`,
`StatusDot`, `EmptyState`, `SettingRow`, `SkeletonCard` + `rememberShimmerAlpha`,
`pressScale`, `motionMillis`, `stateColor`, `statusTextRes`.

## 7. Iconographie

- Icônes Material (variante Rounded) exclusivement — **jamais d'emoji** (les drapeaux
  emoji de la liste de pays font exception : données, pas décoration).
- Logo : `res/drawable/ic_logo_gv.xml` — symbole officiel (anneau en goutte + V,
  dégradés #246BFD→#21D4FD ; référence : `brand/logo_official.png`, voir `brand/README.md`).

## 8. Honnêteté des données (règle transversale)

- Mesure réelle existante → valeur + unité.
- Mesure non disponible → « Indisponible » (jamais de zéro trompeur).
- Donnée de démonstration → valeur + `DemoBadge` + note de pied d'écran.
- « Connexion sécurisée » uniquement en état `Connected` de la machine à états.
