#!/bin/bash

ACCEPTANCE_TEST_CLASSES=$(find flute-acceptance/src/main/ -name '*Test.java'|sed -e 's/.java//g'|sed -e 's/\//\./g'|sed -e 's/flute-acceptance.src.main.//g' |tr "\n" " ")
JAVA_AGENT_ARGS="-javaagent:flute-agent/build/libs/flute-agent-all-0.1.4.jar=$(readlink -f flute-integration/src/test/resources/acceptance-test-flute.properties)"

java -Djava.net.preferIPv4Stack=true $JAVA_AGENT_ARGS -Dlog4j.configuration=file://`pwd`/flute-acceptance/src/main/resources/logging/log4j.xml -cp flute-acceptance/build/libs/flute-acceptance-all-0.1.4.jar org.junit.runner.JUnitCore $ACCEPTANCE_TEST_CLASSES
