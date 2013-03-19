#!/bin/bash
export MAVEN_OPTS="-Xms1024M -Xmx3072M -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
cd ..
mvn clean install -P h2,jetty -Dmaven.test.skip=true

