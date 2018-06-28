#!/bin/bash

set -ex

VERSION="$1"
SCRIPT_DIR="$(dirname $0)"
PROJECT_ROOT="$(readlink -f $SCRIPT_DIR/../)"

cd $PROJECT_ROOT

./gradlew bundleJar

ARTEFACT_DIR="$PROJECT_ROOT/artefacts"
ARTEFACT_NAME="$ARTEFACT_DIR/flute-server-bundle-$VERSION.zip"

TMP_DIR=$(mktemp -d)
LIB_DIR="$TMP_DIR/lib"
BIN_DIR="$TMP_DIR/bin"
SCRIPT_DIR="$TMP_DIR/script"
ASSET_DIR="$TMP_DIR/assets"
mkdir $LIB_DIR
mkdir $BIN_DIR
mkdir $SCRIPT_DIR
mkdir -p $ARTEFACT_DIR
mkdir -p $ASSET_DIR
rm "$ARTEFACT_NAME"

cp "./flute-persistor/build/libs/flute-persistor-all-$VERSION.jar" $BIN_DIR
cp "./flute-server/build/libs/flute-server-all-$VERSION.jar" $BIN_DIR

cp scripts/start-metric-server.sh $SCRIPT_DIR
cp scripts/start-persistor.sh $SCRIPT_DIR
cp scripts/start-report-admin-server.sh $SCRIPT_DIR

cp config/metrics-server-template.properties $SCRIPT_DIR
cp config/report-server-template.properties $SCRIPT_DIR
cp config/persistor-template.properties $SCRIPT_DIR

cp config/logback.xml $SCRIPT_DIR

cp -r flute-server/src/main/resources/ui $ASSET_DIR/

sed -i -e "s/FLUTE_VERSION/$VERSION/g" $SCRIPT_DIR/*.sh

cd $TMP_DIR
zip -qr $ARTEFACT_NAME ./*
cd $PROJECT_ROOT

rm -rf $TMP_DIR
