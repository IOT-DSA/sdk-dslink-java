# sdk-dslink-java

[![Build Status](https://travis-ci.org/IOT-DSA/sdk-dslink-java.svg?branch=master)](https://travis-ci.org/IOT-DSA/sdk-dslink-java)

Java binding for the DSA API.

## Running the examples

In order to run any examples a broker must be running. All the examples can
quickly be ran through Gradle. The `-d` is required because the `dslink.json`
is not available in the build folder.

Running the requester: <br />
`./gradlew :subprojects/requester:run -Dexec.args="-b http://localhost:8080/conn -d ../dslink.json"`

Running the responder: <br />
`./gradlew :subprojects/responder:run -Dexec.args="-b http://localhost:8080/conn -d ../dslink.json"`

## Acknowledgements

A special thanks to JProfiler for supporting this project!

[![](https://www.ej-technologies.com/images/product_banners/jprofiler_small.png)](http://www.ej-technologies.com/products/jprofiler/overview.html)
