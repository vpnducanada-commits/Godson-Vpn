# GODSON VPN — Projet complet

> **Votre connexion. Votre contrôle.**
> VPN Android premium, rapide et centré sur la confidentialité. Kotlin · Jetpack Compose · WireGuard.

---

## ⚡ État réel (vérifié, daté)

| Phase | Contenu | Statut |
|---|---|---|
| 1 | Analyse & architecture | ✅ Terminée |
| 2 | Identité visuelle, design system, écrans (refonte premium) | ✅ Terminée |
| 3 | Fonctionnement applicatif : **police Inter**, **i18n natif FR/EN/PT** (220 clés × 3, parité vérifiée), **tunnelage fractionné** | ✅ Terminée |
| 4 | Backend & catalogue réels — **code complet et testé** (`backend/server.py` + installateur VPS en 1 commande, `docs/09`) | 🟢 Déploiement restant (VPS + domaine) |
| 5 | Intégration VPN réelle (WireGuard/VpnService) — **code compilé**, split tunneling câblé (vérifié au bytecode de la lib) | 🟠 Test sur serveur restant |
| 6 | Tests & sécurité — **61 tests unitaires verts en CI**, manifeste minimal (allowBackup=false, cleartext interdit), 0 secret/log sensible | 🟡 Instrumentés + fuites restants (appareil/backend) |
| 7 | Build, signature, publication — **CI : APK debug à chaque push + job release (APK/AAB signés par secrets)**, kit Play (docs/08) | 🟡 Compte Play + hébergement restants |

**Chaîne de compilation** : GitHub Actions (JDK 17 Temurin + SDK 36) — verte.
**Version** : `0.3.0-phase3`.

## 📱 Essayer maintenant

1. Onglet **Actions** du dépôt → dernier run vert → artefact **`app-debug-apk`**.
2. Installer (Android 8.0+). L'app démarre en **mode démonstration honnête** :
   catalogue étiqueté DÉMO, GODSON PULSE (évaluation déterministe), favoris/récents,
   split tunneling (réglages persistés), FR/EN/PT (langue par application, Android 13+).
3. Toute tentative de connexion affiche l'état **réel** (backend absent) — jamais de
   faux « Connecté ». C'est voulu.

## 🌍 Passer en VPN RÉEL

Tout est prêt : **`docs/09_BACKEND_SERVEUR_REEL.md`** (VPS ~4-5 €/mois ou Oracle Free,
domaine gratuit DuckDNS, **une commande** d'installation, puis bascule `API_BASE_URL` →
APK reconstruit en mode réel par la CI). Le backend (`backend/`) est testé de bout en
bout (sessions = pairs WireGuard révocables, contrat conforme aux DTO de l'app).

## 🔐 Sécurité — principes appliqués

- Clé privée WireGuard générée **sur l'appareil**, jamais transmise, jamais journalisée.
- TLS automatique (Let's Encrypt) côté backend ; transports HTTPS uniquement.
- Permissions minimales ; `allowBackup=false` ; aucun log de données sensibles en release.
- Étiquetage strict démo/réel (`data/AppMode.kt`) : aucune donnée simulée présentée comme réelle.

## 🗂️ Structure

```
android/          Application Kotlin/Compose (module :app), tests JUnit (61)
backend/          Backend v1 (Python stdlib) + installateur VPS + catalogue serveurs
docs/01…09        Architecture, plan, design, infra WireGuard, build, audit, PULSE,
                  Termux, publication Play, VPN réel
brand/            Identité (logo officiel, SVG maîtres)
demo/             Maquette HTML interactive (design uniquement, étiquetée)
.github/workflows CI (build debug + release signée manuelle)
```

## 📚 Documentation clé

- **docs/02** : plan de réalisation détaillé (statuts par chantier, preuves).
- **docs/04** : infrastructure WireGuard exacte. **docs/05** : build/tests locaux.
- **docs/07** : compiler sur le téléphone (Termux). **docs/08** : signature + Play Store.
- **docs/09** : mettre un vrai serveur en ligne en une commande.

---

*Aucun faux état « connecté », aucune donnée simulée présentée comme réelle — par conception.*
