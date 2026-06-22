# Parkar

Application Android pour retrouver facilement où vous avez garé votre voiture.

[![Télécharger l'APK](https://img.shields.io/badge/T%C3%A9l%C3%A9charger-Parkar.apk-blue?style=for-the-badge&logo=android)](https://github.com/GraphyMotion/Parkar/releases/latest/download/Parkar.apk)

## Installation

1. Cliquez sur le bouton **Télécharger** ci-dessus depuis votre téléphone Android (ce lien pointe toujours vers la dernière version)
2. Ouvrez le fichier `Parkar.apk` téléchargé
3. Si Android affiche un avertissement "éditeur non reconnu" : c'est normal pour toute application installée hors du Play Store. Appuyez sur **"Installer quand même"**
4. Si demandé, autorisez "Installer des applications inconnues" pour votre navigateur ou gestionnaire de fichiers

## Fonctionnalités

- Gestion de plusieurs voitures avec photo
- Enregistrement du parking (position GPS, adresse, photos, note, rappel)
- Carte OpenStreetMap et boussole pour retrouver sa voiture
- Bluetooth Auto-Park : sauvegarde automatique de la position à la déconnexion du Bluetooth de la voiture
- Widget écran d'accueil
- Mise à jour intégrée à l'application (écran *Mes voitures* → carte "Mises à jour")

## Mises à jour

L'application vérifie automatiquement les nouvelles versions et peut les télécharger/installer directement. Vous pouvez aussi vérifier manuellement depuis l'écran **Mes voitures**.

## Compilation depuis les sources

```
git clone https://github.com/GraphyMotion/Parkar.git
cd Parkar
./gradlew assembleDebug
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/`.
