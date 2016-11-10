#!/usr/bin/env bash

wget https://storage.googleapis.com/dart-archive/channels/stable/release/latest/sdk/dartsdk-linux-x64-release.zip
unzip dartsdk-linux-x64.zip
alias pub=dart-sdk/bin/pub
alias dart=dart-sdk/bin/dart

# Deploy to maven
if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo 'Not in a pull request, therefore not running integrated tests.'
    exit
fi

BRANCH_NAME = $TRAVIS_PULL_REQUEST_BRANCH

git clone https://github.com/IOT-DSA/dslink-dart-test
cd dslink-dart-test/
pub get

dart tool/grind.dart run-tests-for-sdk-pull-request
