
[![Build Status](https://travis-ci.org/onaio/zebra-lite.svg?branch=master)](https://travis-ci.org/onaio/zebra-lite)


# zebra-lite

![zebra](https://camo.githubusercontent.com/4227308e3e537c2db6c1ed0f92c3290a88e74985/68747470733a2f2f692e696d6775722e636f6d2f7a776b7475374d2e706e67)

## Overview
This is a light version of Zebra that allows users to log into their accounts, view their forms, and view the data in their forms. It uses the Clojure(Script) Ona client [milia](https://github.com/onaio/milia) to connect to the [Ona API](http://ona.io/api/). It connections to [Ona Production](http://ona.io/) by default but can be [configured](https://github.com/onaio/zebra-lite#configuration) to connect to any server running Ona or an Ona-compatible API.

## Component Architecture
+ Login
+ Forms View
+ Submission View (using [hatti](https://github.com/onaio/hatti))
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
