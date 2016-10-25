#!/bin/bash

ACCEPTANCE_TEST_CLASSES=$(find acceptance/src/main/ -name '*Test.java'|sed -e 's/.java//g'|sed -e 's/\//\./g'|sed -e 's/acceptance.src.main.//g' |tr "\n" " ")
JAVA_AGENT_ARGS="-javaagent:agent/build/libs/agent-all-0.1.1.jar=$(readlink -f integration/src/test/resources/acceptance-test-flute.properties)"


java $JAVA_AGENT_ARGS -cp acceptance/build/libs/acceptance-all-0.1.1.jar org.junit.runner.JUnitCore $ACCEPTANCE_TEST_CLASSES
