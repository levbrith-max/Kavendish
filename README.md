# VivoGameSpace — Extended

Fork amélioré de l'application **Vivo Game Space** (com.vivo.game), déverrouillé pour tous les appareils Android et enrichi de nouvelles fonctionnalités.

[![Build APK](https://github.com/YOUR_USERNAME/VivoGameSpace/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/VivoGameSpace/actions/workflows/build.yml)

---

## Nouvelles fonctionnalités

### 1.  Ajouter un jeu
Bouton en bas à gauche de l'écran principal (même style que les boutons originaux de l'app).
- Parcourir toutes les applications installées sur l'appareil
- Recherche par nom
- Ajout en un tap — l'icône du package Android s'affiche dans la grille

### 2.  Ajouter une collection
Juste en dessous du bouton "Ajouter un jeu".
- Sélectionner un fichier `metadata.txt` ou `metadata.pegasus.txt`
- Import automatique de toutes les collections et tous les jeux du fichier
- Les jeux s'affichent avec leurs `assets.logo` Pegasus comme icône
- Lancement via la commande `launch:` du fichier metadata (supporte `{file.path}` et autres placeholders)
- Compatible avec les apps Android (`file: android:com.package.name`) et les ROMs (`file: rom.iso`)

### 3.  Nettoyage automatique à la sortie
Quand l'utilisateur quitte l'application (retour launcher, bouton Home), toutes les activités en cours sont fermées proprement — aucune activité ne reste suspendue en arrière-plan.

### 4.  Compatible tous appareils
L'APK original requérait un appareil Vivo. Ce fork fonctionne sur n'importe quel Android ≥ 6.0 (API 23).

---

## Format Pegasus supporté

```
collection: NomDeLaCollection
shortname: short
assets.logo: dossier/logo.png
launch: am start
 -n com.emulateur/com.emulateur.MainActivity
 -e fichier {file.path}

game: Titre du jeu
file: chemin/vers/rom.iso
assets.logo: dossier/icone.png
assets.steam: dossier/header.png
assets.background: dossier/background.png
developer: Studio
description: Description du jeu
genre: Action

game: App Android
file: android:com.exemple.app
launch: am start -n com.exemple.app/.MainActivity
assets.logo: dossier/logo.png
```

### Placeholders supportés dans `launch:`
| Placeholder | Remplacement |
|-------------|-------------|
| `{file.path}` | Chemin absolu du fichier |
| `{file.dir}` | Dossier parent du fichier |
| `{file.name}` | Nom du fichier avec extension |
| `{file.stem}` | Nom du fichier sans extension |

### Priorité des icônes
1. `assets.logo` du jeu dans le fichier metadata (chemin relatif au dossier du fichier metadata)
2. `assets.steam` / `assets.poster` (fallback)
3. Icône du package Android (via PackageManager)
4. Icône générique

---

## Build

### Pré-requis
- Android Studio Hedgehog ou supérieur
- JDK 17
- SDK Android 34

### Build local
```bash
./gradlew assembleDebug
# APK dans : app/build/outputs/apk/debug/
```

### Build via GitHub Actions
1. Forker ce dépôt
2. Pousser sur `main` → build debug automatique
3. Créer un tag `v1.0.0` → build release + création de Release GitHub

### Signature release (optionnel)
Ajouter ces secrets dans les paramètres du dépôt GitHub :
| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Keystore encodé en base64 (`base64 keystore.jks`) |
| `STORE_PASSWORD` | Mot de passe du keystore |
| `KEY_ALIAS` | Alias de la clé |
| `KEY_PASSWORD` | Mot de passe de la clé |

---

## Architecture

```
app/src/main/kotlin/com/vivo/game/
├── GameApplication.kt                    # Application + nettoyage auto
├── gamespace/
│   ├── spirit/
│   │   ├── CustomGameEntry.kt            # Entité Room (jeux personnalisés)
│   │   ├── CustomGameDao.kt              # DAO Room
│   │   ├── GameSpaceDatabase.kt          # Base de données Room
│   │   ├── CustomGameRepository.kt       # Repository
│   │   └── CustomGameViewModel.kt        # ViewModel
│   ├── ui/
│   │   ├── GameSpaceSplashActivity.kt    # Splash screen
│   │   ├── GameSpaceHostActivity.kt      # Hôte principal
│   │   ├── GameSpaceMyGameFragment.kt    # Fragment principal (étendu)
│   │   ├── PrivacySettingsActivity.kt    # Paramètres
│   │   ├── add/
│   │   │   └── AddGameActivity.kt        # Ajouter une app installée
│   │   ├── collection/
│   │   │   └── AddCollectionActivity.kt  # Importer Pegasus
│   │   └── widget/
│   │       └── CustomGameAdapter.kt      # Adapteur grille gaming
│   └── utils/
│       ├── ActivityStack.kt              # Suivi + nettoyage des activités
│       ├── PegasusMetadataParser.kt      # Parser format Pegasus
│       └── GameLauncher.kt              # Lancement jeux (package / shell)
```

---

## Permissions requises

| Permission | Usage |
|-----------|-------|
| `INTERNET` | Chargement de contenu en ligne |
| `QUERY_ALL_PACKAGES` | Lister les apps installées |
| `READ_EXTERNAL_STORAGE` | Lire les fichiers metadata et assets |
| `READ_MEDIA_IMAGES` | Accès aux images (Android 13+) |
| `FOREGROUND_SERVICE` | Service en avant-plan |

---

## Contribution

Les PR sont les bienvenues ! Pour les nouvelles fonctionnalités, ouvrir d'abord une issue.

## Licence

Ce projet est un fork non officiel de l'app Vivo Game Space, créé à des fins éducatives et de personnalisation. Vivo est propriétaire des assets originaux.
