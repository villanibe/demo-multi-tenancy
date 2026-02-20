#!/bin/sh
# Maven Wrapper shell script
#
# This script is a minimal wrapper that delegates to the Maven distribution shipped in .mvn/wrapper.
# It keeps the repo self-contained and consistent across environments.

set -eu

BASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

exec "$BASEDIR/.mvn/wrapper/apache-maven-3.9.9/bin/mvn" "$@"
