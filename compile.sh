#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
JAVAFILES="$( find $DIR/src -name '*.java' -print )"
javac -source 6 -target 6 -bootclasspath $HOME/jre1.6.0_06-p/lib/rt.jar -Xlint:unchecked -sourcepath $DIR/src -classpath $DIR/bin -d $DIR/bin $JAVAFILES
