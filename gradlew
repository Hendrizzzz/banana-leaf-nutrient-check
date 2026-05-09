#!/bin/sh

##############################################################################
##
##  Gradle startup script for POSIX generated for this project skeleton.
##
##############################################################################

PRG="$0"
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")"/$link"
    fi
done

APP_HOME=$(dirname "$PRG")
APP_NAME=$(basename "$0")

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

if [ -n "$JAVA_HOME" ] && [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

eval set -- $DEFAULT_JVM_OPTS "$JAVA_OPTS" "$GRADLE_OPTS" '"-Dorg.gradle.appname=$APP_NAME"' -classpath '"$CLASSPATH"' org.gradle.wrapper.GradleWrapperMain "$@"
exec "$JAVACMD" "$@"
