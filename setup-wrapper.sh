#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TARGET="$ROOT/gradle/wrapper/gradle-wrapper.jar"
URL="https://github.com/FabricMC/fabric-example-mod/raw/refs/heads/1.21.1/gradle/wrapper/gradle-wrapper.jar"
mkdir -p "$(dirname "$TARGET")"
if command -v curl >/dev/null 2>&1; then
  curl -fL "$URL" -o "$TARGET"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$TARGET" "$URL"
else
  echo "curl or wget is required to download gradle-wrapper.jar" >&2
  exit 1
fi
echo "Wrapper saved to $TARGET"
