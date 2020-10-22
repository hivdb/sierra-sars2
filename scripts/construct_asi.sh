#! /bin/sh

set -e

cd "$(dirname $0)/.."

pipenv run python scripts/construct_asi.py SARS2 --target-version latest
