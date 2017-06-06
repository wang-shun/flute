#!/bin/bash
CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-flute.properties

echo "CONFIG_FILE: $CONFIG_FILE"

java -cp flute-integration/build/libs/flute-integration-all-0.1.4.jar com.aitusoftware.flute.integration.client.TimingClientMain $CONFIG_FILE
