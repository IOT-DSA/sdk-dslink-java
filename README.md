# sdk-dslink-java

[![Build Status](https://travis-ci.org/IOT-DSA/sdk-dslink-java.svg?branch=master)](https://travis-ci.org/IOT-DSA/sdk-dslink-java)

Java binding for the DSA API.

## Running the examples

In order to run any examples a broker must be running. All the examples can quickly be ran through gradle.

Running the requester: <br />
`./gradlew :subprojects/requester:run -Dexec.args="--broker http://localhost:8080/conn"`

Running the responder: <br />
`./gradlew :subprojects/responder:run -Dexec.args="--broker http://localhost:8080/conn"`
