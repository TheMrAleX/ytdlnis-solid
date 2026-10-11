#!/usr/bin/env bash
# Run on your computer, with GitHub CLI authenticated to TheMrAleX.
set -euo pipefail
repo="TheMrAleX/ytdlnis-solid"
command -v gh >/dev/null || { echo "Install GitHub CLI first."; exit 1; }
command -v python3 >/dev/null || { echo "Install Python 3 first."; exit 1; }
command -v keytool >/dev/null || { echo "Install JDK 17 first."; exit 1; }
existing=$(gh secret list --repo "$repo" --json name --jq '.[].name')
if [[ "$existing" == *BASE_64_SIGNING_KEY* || "$existing" == *LOCAL_PROPERTIES* ]]; then
  echo "Signing secrets already exist. Reuse the original key; do not replace it."
  exit 1
fi
backup_dir="${PWD}/solid-signing-private"
if [[ -e "$backup_dir" ]]; then
  echo "Private backup already exists. Preserve and reuse it."
  exit 1
fi
umask 077
mkdir "$backup_dir"
read -rsp "New permanent keystore password (at least 6 characters): " SOLID_STORE_PASSWORD
printf '\n'
if (( ${#SOLID_STORE_PASSWORD} < 6 )); then
  echo "Password must contain at least 6 characters."; rmdir "$backup_dir"; exit 1
fi
export SOLID_STORE_PASSWORD
keytool -genkeypair -keystore "$backup_dir/release_keystore.jks" -storetype JKS \
  -alias solid -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass:env SOLID_STORE_PASSWORD -keypass:env SOLID_STORE_PASSWORD \
  -dname "CN=YTDLnis Solid, OU=Android, O=TheMrAleX"
python3 - "$backup_dir/keystore.properties" <<'PYPROPS'
import json, os, sys
from pathlib import Path
password = os.environ['SOLID_STORE_PASSWORD']
password = json.dumps(password, ensure_ascii=True)[1:-1].replace(' ', chr(92)+' ')
Path(sys.argv[1]).write_text('signingConfig.storeFile=release_keystore.jks'+chr(10)+
    'signingConfig.storePassword='+password+chr(10)+
    'signingConfig.keyAlias=solid'+chr(10)+
    'signingConfig.keyPassword='+password+chr(10))
PYPROPS
base64 < "$backup_dir/release_keystore.jks" | tr -d '\n' | gh secret set BASE_64_SIGNING_KEY --repo "$repo"
gh secret set LOCAL_PROPERTIES --repo "$repo" < "$backup_dir/keystore.properties"
unset SOLID_STORE_PASSWORD
echo "Permanent signing configured. Back up solid-signing-private securely, then run Solid APK Releases in Actions."
