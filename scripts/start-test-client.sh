#!/bin/bash
CONFIG_FILE=integration/src/test/resources/acceptance-test-flute.properties

echo "CONFIG_FILE: $CONFIG_FILE"

java -cp integration/build/libs/flute-integration-all-0.1.2.jar com.aitusoftware.flute.integration.client.TimingClientMain $CONFIG_FILE
