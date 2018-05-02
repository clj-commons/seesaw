#!/bin/sh

JAVA_OPTS="-XX:MaxPermSize=512m -Xmx512m"
java $JAVA_OPTS -cp `lein classpath` lazytest.main test
