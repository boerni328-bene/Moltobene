#!/usr/bin/env bash
# Erzeugt die Beschreibung eines GitHub-Releases (#13, #21):
# Kernsatz, Abschnitt der Version aus CHANGELOG.md, Hinweis zur Installation und Angaben zur Echtheit.
# Aufruf: release-text.sh <versionName> <apk-datei>   (die APK darf fehlen, dann ohne Echtheitsangaben)
set -euo pipefail

VERSION="$1"
APK="$2"
KERNSATZ="Moltobene sammelt Kochrezepte aus allen Quellen an einem Ort, gespeichert nur auf dem eigenen Handy."

# Abschnitt dieser Version aus CHANGELOG.md herausschneiden (bis zur nächsten Überschrift „## [“).
ABSCHNITT=$(awk -v v="$VERSION" '
  /^## \[/ { if (found) exit; if (index($0, "## [" v "]") == 1) { found = 1; next } }
  found { print }
' CHANGELOG.md | sed '/./,$!d')
if [ -z "${ABSCHNITT//[[:space:]]/}" ]; then ABSCHNITT="Version $VERSION"; fi

echo "_${KERNSATZ}_"
echo
echo "$ABSCHNITT"
echo
echo "### Installation"
echo
echo "Die Datei \`moltobene-$VERSION.apk\` herunterladen und auf dem Handy öffnen (ab Android 8.0)."
echo "Ein Update wird einfach über die vorhandene App installiert, die Sammlung bleibt dabei erhalten."

if [ -f "$APK" ]; then
  PRUEFSUMME=$(sha256sum "$APK" | cut -d' ' -f1)
  APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -n 1)apksigner"
  FINGERABDRUCK=$("$APKSIGNER" verify --print-certs "$APK" \
    | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' \
    | tr 'a-f' 'A-F' | sed 's/../&:/g; s/:$//')
  echo
  echo "### Echtheit prüfen"
  echo
  echo "- Prüfsumme (SHA-256) der APK: \`$PRUEFSUMME\`"
  echo "- Fingerabdruck des Signaturzertifikats (SHA-256): \`$FINGERABDRUCK\`"
  echo
  echo "Wie man beides prüft, steht in der [README](https://github.com/$GITHUB_REPOSITORY#echtheit-pr%C3%BCfen)."
fi
