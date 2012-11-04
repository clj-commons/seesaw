#!/bin/sh

JAVA_OPTS="-XX:MaxPermSize=512m -Xmx512m"
java $JAVA_OPTS -cp `lein2 classpath` lazytest.watch src test

