#!/usr/bin/env bash

# Deploy to maven
if [ "$TRAVIS_BRANCH" != 'master' ]; then
    echo 'Ignoring artifact upload (not on master branch)'
elif [ "$TRAVIS_PULL_REQUEST" == 'true' ]; then
    echo 'Ignoring artifact upload (pull request)'
else
    if [ -z "$USER" ]; then
        echo "User is empty"
    fi
    if [ -z "$PASS" ]; then
        echo "Passowrd is empty"
    fi
    echo 'Deploying to maven'
    ./gradlew "-Psigning.secretKeyRingFile=$PWD/$LOC" \
                "-Psigning.keyId=$ID" \
                "-Psigning.password=$KEY_PASS" \
                "-PossrhUsername=$USER" \
                "-PossrhPassword=$PASS" \
                build \
                uploadArchives
fi
