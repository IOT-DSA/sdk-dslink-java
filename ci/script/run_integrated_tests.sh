#!/usr/bin/env bash

wget https://storage.googleapis.com/dart-archive/channels/stable/release/latest/sdk/dartsdk-linux-x64-release.zip
unzip dartsdk-linux-x64.zip
CURRENT_DIR="$(pwd)"

# Deploy to maven
if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Not in a pull request, therefore not running integrated tests.'
    exit
fi

BRANCH_NAME = $TRAVIS_PULL_REQUEST_BRANCH

git clone https://github.com/IOT-DSA/dslink-dart-test
cd dslink-dart-test/
$CURRENT_DIR/dart-sdk/bin/pub get

$CURRENT_DIR/dart-sdk/bin/dart tool/grind.dart run-tests-for-sdk-pull-request
