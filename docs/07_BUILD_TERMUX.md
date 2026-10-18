# 07 — Compiler GODSON VPN sur Android avec Termux

> **Statut** : procédure préparée, non testée sur appareil (aucun Android dans l'environnement
> de développement). Elle s'appuie sur des faits vérifiés du projet : le build Gradle est
> 100 % JVM (Kotlin, d8/R8, packaging) sauf **un** binaire (`aapt2`), que Termux fournit en
> version ARM native et qu'AGP permet de substituer. Les 51 tests et la compilation Kotlin
> sont **déjà prouvés** sur une machine plus faible qu'un téléphone récent.

## 0. Prérequis et attentes honnêtes

| Élément | Exigence |
|---|---|
| Termux | **Depuis F-Droid obligatoirement** (la version Play Store est obsolète et cassée) |
| RAM du téléphone | 4 Go = tentative (options mémoire réduites) ; 6 Go+ = confortable |
| Espace libre | ~3 Go (JVM ~350 Mo + SDK ~150 Mo + caches Gradle ~1,5 Go) |
| Durée 1er build | 15 à 45 min (téléchargements ~1 Go + compilation) |
| Acode | Éditeur de texte **uniquement** — ne compile rien, non nécessaire ici |
| Risque principal | Manque de RAM à l'étape `mergeExtDexDebug` (fusion des DEX) — le même mur a été atteint sur un bac à sable de 1,9 Go ; les téléphones utilisent toutefois zram/swap, ce qui peut suffire |

**Pendant tout le build** : garder Termux **au premier plan**, téléphone en charge,
`termux-wake-lock` actif — Android tue les applications en arrière-plan.

## 1. Installation de base (copier-coller dans Termux)

```bash
pkg update -y && pkg upgrade -y
pkg install -y openjdk-17 aapt2 apksigner unzip
termux-wake-lock
java -version        # attendu : openjdk "17.x"
command -v aapt2     # attendu : …/files/usr/bin/aapt2
```

Si `aapt2` n'est pas trouvé : `pkg search aapt` puis installer le paquet qui le fournit
(historiquement `aapt`), puis revérifier `command -v aapt2`.

## 2. SDK minimal (~60 Mo — platform 36 uniquement)

AGP a besoin de `android.jar` (platform 36) et de la licence acceptée ; il apporte
lui-même d8/R8 en Java. Le binaire `aapt2` vient de Termux (étape 4).

```bash
mkdir -p ~/android-sdk/platforms/android-36 ~/android-sdk/licenses
curl -L -o /tmp/p36.zip https://dl.google.com/android/repository/platform-36_r02.zip
unzip -q /tmp/p36.zip -d ~/android-sdk/platforms && rm /tmp/p36.zip
printf '24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n8933bad161af4178b1185d1a37fbf41ea5269c55\n' > ~/android-sdk/licenses/android-sdk-license
```

## 3. Récupérer le projet (archive exportée depuis Arena)

```bash
termux-setup-storage        # accepter la permission d'accès aux fichiers
cd ~
unzip -q ~/storage/downloads/NOM_DE_L_ARCHIVE.zip -d ~
cd ~/GodsonVPN/android
echo "sdk.dir=$HOME/android-sdk" > local.properties
chmod +x gradlew
```

## 4. Le piège classique : pointer AGP vers l'aapt2 ARM de Termux

Sans cela, Gradle télécharge un `aapt2` **x86_64** (inexécutable sur téléphone) et échoue
à `processDebugResources`.

```bash
printf '\n# aapt2 natif ARM de Termux (spécifique à ce téléphone)\nandroid.aapt2FromMavenOverride=%s/bin/aapt2\n' "$PREFIX" >> gradle.properties
```

## 5. Lancer tests + APK

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs="-Xmx2300m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC -Dfile.encoding=UTF-8"
```

- Téléphone **4 Go** : remplacer `-Xmx2300m` par `-Xmx1500m`.
- Résultats attendus : `51 tests, 0 échec` puis `BUILD SUCCESSFUL`.
- APK produit : `app/build/outputs/apk/debug/app-debug.apk`.

## 6. Récupérer et installer l'APK

```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/godson-vpn-debug.apk
```

Puis ouvrir ce fichier depuis le gestionnaire de fichiers → Android demandera
« installer des applications inconnues » (normal pour un APK hors Play Store ;
signature de debug automatique, aucune clé à créer).

## 7. Si échec mémoire (`Daemon disappeared` / `Killed` à mergeExtDexDebug)

1. Relancer (les étapes déjà faites sont en cache — ça reprend où ça s'est arrêté) ;
2. Essayer `-Xmx1500m` (moins de tas, d8 casse moins la machine) ;
3. Fermer toutes les autres applications ;
4. Si le mur persiste : **GitHub Actions reste la voie garantie** (7 Go de RAM côté serveur,
   artefact APK téléchargeable) — le code est déjà prêt pour elle.

## 8. Dépannage rapide

| Symptôme | Cause probable | Remède |
|---|---|---|
| `aapt2 ... finished with non-zero exit` | override manquant ou mauvais chemin | étape 4, vérifier `command -v aapt2` |
| `SDK location not found` | `local.properties` absent | étape 3 |
| `Failed to install the following Android SDK packages... licences` | licence non écrite | étape 2, dernier `echo` |
| `Unsupported class file major version` | JDK ≠ 17 | `pkg install openjdk-17`, vérifier `java -version` |
| Téléchargement Gradle interrompu | réseau mobile | relancer : le wrapper reprend |
