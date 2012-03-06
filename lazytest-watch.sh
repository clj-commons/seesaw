#!/bin/sh

JAVA_OPTS="-XX:MaxPermSize=512m -Xmx512m"
java $JAVA_OPTS -cp "src:test:classes:lib/*:lib/dev/*" lazytest.watch src test

