# VivoGameSpace Extended

Fork ameliore de l'application Vivo Game Space (com.vivo.game), debloque pour tous
les appareils Android et enrichi de nouvelles fonctionnalites.

---

## IMPORTANT - Premiere configuration apres clonage

Apres avoir clone le depot, executer cette commande UNE SEULE FOIS pour que
Git memorise le bit executable de gradlew de facon permanente :

    git update-index --chmod=+x gradlew
    git add gradlew
    git commit -m "chore: mark gradlew as executable"
    git push

Cela evite l'erreur "Permission denied" sur GitHub Actions a chaque build.

---

## Nouvelles fonctionnalites

### Ajouter un jeu
Bouton en bas a gauche de l'ecran principal (meme style que l'app originale).
- Parcourir toutes les applications installees sur l'appareil
- Recherche par nom
- Ajout en un tap — l'icone du package Android s'affiche dans la grille

### Ajouter une collection
Juste en dessous du bouton "Ajouter un jeu".
- Selectionner un fichier metadata.txt ou metadata.pegasus.txt
- Import automatique de toutes les collections et jeux du fichier
- Les jeux s'affichent avec leurs assets.logo Pegasus comme icone
- Lancement via la commande launch: du fichier metadata
- Compatible apps Android (file: android:com.package) et ROMs (file: rom.iso)

### Arriere-plan dynamique
Quand un jeu avec assets.background est selectionne, le fond change avec un
crossfade de 400ms. Sans assets.background, le fond par defaut est restaure.

### Nettoyage automatique a la sortie
Toutes les activites sont fermees quand l'utilisateur quitte l'app.

### Compatible tous appareils
Fonctionne sur n'importe quel Android 6.0+ (API 23), pas uniquement les Vivo.

---

## Format Pegasus supporte

    collection: NomDeLaCollection
    shortname: short
    assets.logo: dossier/logo.png
    launch: am start
     -n com.emulateur/com.emulateur.MainActivity
     -e fichier {file.path}

    game: Titre du jeu
    file: chemin/vers/rom.iso
    assets.logo: dossier/icone.png
    assets.background: dossier/background.png

    game: App Android
    file: android:com.exemple.app
    launch: am start -n com.exemple.app/.MainActivity

Placeholders dans launch:
    {file.path}   Chemin absolu du fichier
    {file.dir}    Dossier parent du fichier
    {file.name}   Nom du fichier avec extension
    {file.stem}   Nom du fichier sans extension

---

## Build local

    ./gradlew assembleDebug
    # APK dans : app/build/outputs/apk/debug/

## Build GitHub Actions

1. Cloner et executer git update-index --chmod=+x gradlew (voir ci-dessus)
2. Pousser sur main -> build debug automatique
3. Creer un tag v1.0.0 -> build release + GitHub Release

Secrets optionnels pour signer le release :
    KEYSTORE_BASE64, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
