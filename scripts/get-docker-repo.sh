#! /bin/bash

BRANCH=`git rev-parse --abbrev-ref HEAD`

if [[ "$BRANCH" == "master" ]]; then
    echo "hivdb/sierra-sars2"
else
    echo "hivdb/sierra-sars2-testing"
fi
