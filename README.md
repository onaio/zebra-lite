# zebra-lite
A lite Zebra (i.e. a baby Zebra)

## Overview
This is a light version of Zebra enabling users to access their forms and submissions through the Ona API. They will not be able to access projects and organisations from this version.

## Component Architecture
+ Login
+ Forms View
+ Submission View
    * table-page
    * map-page
    * chart-page
    * details-page

## Configuration

Remote host configuration is handled by [milia](https://github.com/onaio/milia) using [environ](https://github.com/weavejester/environ#example-usage). Set the keys `:ona-api-server-host` and `:ona-api-server-protocol` to specify the host and protocol, respectively.

## Dependencies and Installation

Building and running require `java` and `leiningen`, the install instructions for leiningen are [here](https://github.com/technomancy/leiningen#installation).

## Running

Run the development server with:

```
lein up
```

Build a deployable production JAR with:

```
lein uberjar
```

Then run that JAR with:

```
java -jar target/zebra-lite-SNAPSHOT-standalone.jar
```

NB: your JAR path may differ if you have altered the configuration.

## License

zebra-lite is copyright 2015 Ona Systems Inc. and is released under the [AGPL License](https://www.gnu.org/licenses/agpl-3.0.html).
