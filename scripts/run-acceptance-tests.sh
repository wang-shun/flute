#!/bin/bash

ACCEPTANCE_TEST_CLASSES=$(find acceptance/src/main/ -name '*Test.java'|sed -e 's/.java//g'|sed -e 's/\//\./g'|sed -e 's/acceptance.src.main.//g' |tr "\n" " ")

java -cp acceptance/build/libs/acceptance-all-0.1.1.jar org.junit.runner.JUnitCore $ACCEPTANCE_TEST_CLASSES
