#!/bin/bash
# Script to copy the provided icon to the project resources

SOURCE_IMAGE="/Users/gokulkrishnapr/.gemini/antigravity/brain/90ceba2b-074e-4975-b0ab-e3612ba4da7a/media__1778593264724.jpg"
RES_DIR="app/src/main/res"

echo "Updating app icons..."

# Create mipmap directories if they don't exist
mkdir -p "$RES_DIR/mipmap-mdpi"
mkdir -p "$RES_DIR/mipmap-hdpi"
mkdir -p "$RES_DIR/mipmap-xhdpi"
mkdir -p "$RES_DIR/mipmap-xxhdpi"
mkdir -p "$RES_DIR/mipmap-xxxhdpi"

# Copy to all mipmap folders as png (renaming from jpg)
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-mdpi/ic_launcher.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-hdpi/ic_launcher.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xhdpi/ic_launcher.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xxhdpi/ic_launcher.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xxxhdpi/ic_launcher.png"

# Copy for round icons too
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-mdpi/ic_launcher_round.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-hdpi/ic_launcher_round.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xhdpi/ic_launcher_round.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xxhdpi/ic_launcher_round.png"
cp "$SOURCE_IMAGE" "$RES_DIR/mipmap-xxxhdpi/ic_launcher_round.png"

# Delete adaptive icon XMLs to ensure PNGs are used
rm -f "$RES_DIR/mipmap-anydpi/ic_launcher.xml"
rm -f "$RES_DIR/mipmap-anydpi/ic_launcher_round.xml"
rm -f "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml"
rm -f "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml"

echo "Done! Please rebuild the project in Android Studio."
