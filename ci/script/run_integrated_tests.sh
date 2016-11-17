#!/usr/bin/env bash

wget https://storage.googleapis.com/dart-archive/channels/stable/release/latest/sdk/dartsdk-linux-x64-release.zip
unzip dartsdk-linux-x64-release.zip
CURRENT_DIR="$(pwd)"

git clone https://github.com/IOT-DSA/dslink-dart-test

cd dslink-dart-test/
${CURRENT_DIR}/dart-sdk/bin/pub get

${CURRENT_DIR}/dart-sdk/bin/dart tool/main.dart run-tests-for-sdk-pull-request --path-to-sdk ${CURRENT_DIR}
