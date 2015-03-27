#!/usr/bin/env bash

# Deploy to maven
if [[ $TRAVIS_BRANCH != 'master' || $TRAVIS_PULL_REQUEST == true ]]; then
    echo 'Ignoring artifact upload'
else
    ./gradlew "-Psigning.secretKeyRingFile=$PWD/$LOC" \
                "-Psigning.keyId=$ID" \
                "-Psigning.password=$KEY_PASS" \
                "-PossrhUsername=$USER" \
                "-PossrhPassword=$PASS" \
                uploadArchives
fi
