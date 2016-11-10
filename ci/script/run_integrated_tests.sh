#!/usr/bin/env bash

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
