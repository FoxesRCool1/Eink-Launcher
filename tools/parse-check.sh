#!/usr/bin/env bash
#
# Check that every Kotlin file parses, without the Android SDK.
#
# Some machines cannot reach the Google Maven repository, so `./gradlew build`
# cannot run there. This script downloads the standalone Kotlin compiler from
# Maven Central and compiles the sources with no Android classes on the
# classpath. Every androidx and android symbol then shows up as "unresolved
# reference", which is expected and is filtered out. What is left is real:
# syntax errors and references to names that do not exist in this project.
#
# It is a smoke test, not a build. CI still does the real build.
#
# Usage: tools/parse-check.sh
#
set -euo pipefail

KOTLIN_VERSION="2.4.20"
COROUTINES_VERSION="1.11.0"
ANNOTATIONS_VERSION="26.0.2"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE="${TMPDIR:-/tmp}/eink-parse-check"
CENTRAL="https://repo.maven.apache.org/maven2"

mkdir -p "$CACHE"

fetch() {
  local name="$1" url="$2"
  if [ ! -s "$CACHE/$name" ]; then
    echo "Fetching $name"
    curl -sS --fail --max-time 300 -o "$CACHE/$name" "$url"
  fi
}

fetch kotlinc.jar     "$CENTRAL/org/jetbrains/kotlin/kotlin-compiler/$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.jar"
fetch stdlib.jar      "$CENTRAL/org/jetbrains/kotlin/kotlin-stdlib/$KOTLIN_VERSION/kotlin-stdlib-$KOTLIN_VERSION.jar"
fetch reflect.jar     "$CENTRAL/org/jetbrains/kotlin/kotlin-reflect/$KOTLIN_VERSION/kotlin-reflect-$KOTLIN_VERSION.jar"
fetch scriptrt.jar    "$CENTRAL/org/jetbrains/kotlin/kotlin-script-runtime/$KOTLIN_VERSION/kotlin-script-runtime-$KOTLIN_VERSION.jar"
fetch annotations.jar "$CENTRAL/org/jetbrains/annotations/$ANNOTATIONS_VERSION/annotations-$ANNOTATIONS_VERSION.jar"
fetch coroutines.jar  "$CENTRAL/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/$COROUTINES_VERSION/kotlinx-coroutines-core-jvm-$COROUTINES_VERSION.jar"

CP="$CACHE/kotlinc.jar:$CACHE/stdlib.jar:$CACHE/reflect.jar:$CACHE/scriptrt.jar:$CACHE/annotations.jar:$CACHE/coroutines.jar"

cd "$ROOT"
# The debug and release source sets both declare DevPanel, so only one of them
# can be on the same compile run.
FILES=$(find app/src/main/kotlin app/src/debug/kotlin app/src/test/kotlin -name '*.kt' | sort)

LOG="$CACHE/compile.log"
set +e
java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -nowarn \
  -cp "$CACHE/stdlib.jar:$CACHE/coroutines.jar" \
  -d "$CACHE/out" $FILES > "$LOG" 2>&1
set -e

SYNTAX=$(grep -cP "error: (expecting|unexpected|unsupported)" "$LOG" || true)

echo
echo "Syntax errors: $SYNTAX"
if [ "$SYNTAX" != "0" ]; then
  grep -P "error: (expecting|unexpected|unsupported)" "$LOG" | head -40
  exit 1
fi

echo "Unresolved names (all of these should be android, androidx, java or test library symbols):"
grep -oP "unresolved reference '\K[^']+" "$LOG" | sort -u | tr '\n' ' ' | fold -w 100
echo
echo
echo "OK: everything parses. Run the real build in CI."
