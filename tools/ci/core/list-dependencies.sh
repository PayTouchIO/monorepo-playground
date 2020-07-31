#!/bin/bash

##
# List all dependencies between projects.
# Poject is identified with relative path to project's root directory from repository root.
#
# Dependencies can be specified in text file where each line is path (from monorepo root) to other project.
# Default location of dependency file is in root dir of each poject on path `.ci/dependencies.txt`.
# Location of dependency file can be changed by setting environment variable `CI_DEPENDENCIES_FILE`.
#
# Outputs lines of tuples in format PROJECT1 PROJECT2 (separated by space),
# where PROJECT1 depends on PROJECT2.
#
# Usage:
#   list-dependencies.sh
##

set -e

# Find script directory (no support for symlinks)
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Configuration with default values
: "${CI_DEPENDENCIES_FILE:=.ci/dependencies.txt}"

# Look for dependecies of each project
for PROJECT in $(${DIR}/list-projects.sh); do
    # Look into dependency file where each row is path to other project
    DEPENDENCIES_FILE="$DIR/../../../$PROJECT/$CI_DEPENDENCIES_FILE"
    if [[ -f $DEPENDENCIES_FILE ]]; then
        for INCLUDE in $(cat $DEPENDENCIES_FILE); do
            echo "$PROJECT $INCLUDE"
        done
    fi
done
