#!/bin/bash

set -ex

VERSION="$1"
SCRIPT_DIR="$(dirname $0)"
PROJECT_ROOT="$(readlink -f $SCRIPT_DIR/../)"

cd $PROJECT_ROOT

./gradlew assemble

ARTEFACT_DIR="$PROJECT_ROOT/artefacts"

TMP_DIR=$(mktemp -d)
LIB_DIR="$TMP_DIR/lib"
SCRIPT_DIR="$TMP_DIR/script"
mkdir $LIB_DIR
mkdir $SCRIPT_DIR
mkdir -p $ARTEFACT_DIR
rm -rf "$ARTEFACT_DIR/*.zip"

cp "./flute-persistor/build/libs/flute-persistor-$VERSION.jar" $LIB_DIR
cp "./flute-server/build/libs/flute-server-$VERSION.jar" $LIB_DIR
cp "./flute-common/build/libs/flute-common-$VERSION.jar" $LIB_DIR

cp scripts/start-metric-server.sh $SCRIPT_DIR
cp scripts/start-persistor.sh $SCRIPT_DIR
cp scripts/start-report-admin-server.sh $SCRIPT_DIR

cp config/metrics-server-template.properties $SCRIPT_DIR
cp config/report-server-template.properties $SCRIPT_DIR
cp config/persistor-template.properties $SCRIPT_DIR

sed -i -e "s/FLUTE_VERSION/$VERSION/g" $SCRIPT_DIR/*.sh

cd $TMP_DIR
zip -qr "$ARTEFACT_DIR/flute-server-bundle-$VERSION.zip" ./*
cd $PROJECT_ROOT

rm -rf $TMP_DIR