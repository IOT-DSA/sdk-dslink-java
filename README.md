# sdk-dslink-java

[![](https://jitpack.io/v/iot-dsa/sdk-dslink-java.svg)](https://jitpack.io/#iot-dsa/sdk-dslink-java)

Java binding for the DSA API.

## Project Structure

There are four categories that each Gradle subproject fall under. Those categories are:

- sdk
- examples
- runtimes
- internal

Each categorized directory has its own README describing its category in more detail.

## Running the examples

In order to run any examples a broker must be running. All the examples can quickly be ran through
Gradle.

Running the requester: <br />
`./gradlew :examples/requester:run -Dexec.args="-b http://localhost:8080/conn"`

Running the responder: <br />
`./gradlew :examples/responder:run -Dexec.args="-b http://localhost:8080/conn"`

## Dependency Management

Maven

```
<repositories>
    <repository>
        <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
    <groupId>com.github.iot-dsa</groupId>
    <artifactId>sdk-dslink-java</artifactId>
    <version>Tag</version>
</dependency>
```

Gradle

```
allprojects {
    repositories {
	    ...
	    maven { url 'https://jitpack.io' }
	}
}

dependencies {
    implementation 'com.github.iot-dsa:sdk-dslink-java:Tag'
}
```