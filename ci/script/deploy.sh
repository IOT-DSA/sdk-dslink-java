#!/usr/bin/env bash

# Deploy to maven
if [ "$TRAVIS_BRANCH" != "master" ]; then
    echo 'Ignoring artifact upload (not on master branch)'
elif [ "$TRAVIS_PULL_REQUEST" == "true" ]; then
    echo 'Ignoring artifact upload (pull request)'
else
    if [ -z "$BINTRAY_USER" ]; then
        echo "Bintray User is empty"
    fi
    if [ -z "$BINTRAY_KEY" ]; then
        echo "Bintray Key is empty"
    fi
    if [ -z "$OSSRH_USER" ]; then
        echo "OSSRH User is empty"
    fi
    if [ -z "$OSSRH_PASS" ]; then
        echo "OSSRH Password is empty"
    fi
    echo 'Deploying to jcenter and maven'
    ./gradlew "-PbintrayUser=$BINTRAY_USER" \
                "-PbintrayApiKey=$BINTRAY_KEY" \
                "-PossrhUser=$OSSRH_USER" \
                "-PossrhPass=$OSSRH_PASS" \
                build \
                bintrayUpload
fi
