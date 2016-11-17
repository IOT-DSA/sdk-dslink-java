# sdk-dslink-java
 tyest
[![Build Status](https://travis-ci.org/IOT-DSA/sdk-dslink-java.svg?branch=master)](https://travis-ci.org/IOT-DSA/sdk-dslink-java)

Java binding for the DSA API.

## Project Structure

There are four categories that each Gradle subproject fall under. Those categories
are:
- sdk
- examples
- runtimes
- internal

Each categorized directory has its own README describing its category in more detail.

## Running the examples

In order to run any examples a broker must be running. All the examples can
quickly be ran through Gradle.

Running the requester: <br />
`./gradlew :examples/requester:run -Dexec.args="-b http://localhost:8080/conn"`

Running the responder: <br />
`./gradlew :examples/responder:run -Dexec.args="-b http://localhost:8080/conn"`

## Acknowledgements

A special thanks to JProfiler for supporting this project!

[![](https://www.ej-technologies.com/images/product_banners/jprofiler_small.png)](http://www.ej-technologies.com/products/jprofiler/overview.html)
