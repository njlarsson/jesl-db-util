#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
JAVAFILES="$( find $DIR/src -name '*.java' -print )"
javac -Xlint:unchecked -sourcepath $DIR/src -classpath $DIR/bin -d $DIR/bin $JAVAFILES
