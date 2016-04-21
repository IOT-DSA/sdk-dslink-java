#!/usr/bin/env bash

# Perform a raw build
./gradlew build distZip
cat file:///home/travis/build/IOT-DSA/sdk-dslink-java/sdk/historian/build/reports/findbugs/main.html
