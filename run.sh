#!/usr/bin/env bash
./gradlew run --quiet -I run_log.gradle -PappArgs="codegen $*"
