# Identité visuelle GODSON VPN

**Logo officiel validé** : `logo_official.png` (référence maîtresse, 2048×2048).
Le symbole associe un **anneau en goutte** (évoquant à la fois le G, le bouclier et
l'emplacement de connexion) et un **V central**, en dégradé bleu #246BFD → cyan #21D4FD.
Wordmark : GODSON en blanc doux #F5F8FF, VPN en cyan #21D4FD, capitales espacées.

## Fichiers

| Fichier | Usage |
|---|---|
| `logo_official.png` | **Référence maîtresse** fournie (à conserver, ne pas retoucher) |
| `logo_gv.svg` | Logo principal : symbole + wordmark (fonds sombres) |
| `logo_gv_symbol.svg` | Symbole seul (splash, favicon, chargement) |
| `logo_gv_mono_light_bg.svg` | Version monochrome pour fonds clairs (#0B1730) |
| `logo_gv_mono_dark_bg.svg` | Version monochrome pour fonds sombres (#F5F8FF) |

Les SVG sont des **vectorisations fidèles** du visuel fourni (mêmes formes, mêmes
dégradés) : ils permettent l'affichage net à toutes les tailles et l'intégration
Android. En cas de doute, le PNG fait foi.

## Dans le projet Android

- `res/drawable/ic_logo_gv.xml` : symbole officiel avec dégradés (utilisé dans
  le splash, l'en-tête, l'onboarding via Compose).
- `res/drawable/ic_launcher_foreground.xml` : icône adaptative (calque principal
  gradient + couche monochrome système).
- `res/drawable/ic_stat_shield.xml` : icône de notification monochrome (alpha).
- `res/mipmap-anydpi-v26/ic_launcher.xml` : fond #07111F + calque principal.

## Règles d'usage

1. Zone de protection : ≥ 20 % de la largeur du symbole autour du logo.
2. Taille minimale : 24 dp (symbole), 120 px (logo complet avec wordmark).
3. Ne pas déformer, recolorer hors palette officielle, ajouter d'ombre externe
   forte ni faire tourner le symbole.
4. Dégradés autorisés uniquement : #246BFD→#21D4FD (anneau), #2E5BFF→#21C9FD (V).
5. Monochromes autorisés : #0B1730 (fond clair) et #F5F8FF (fond sombre).
