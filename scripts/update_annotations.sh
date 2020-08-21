#! /bin/bash

set -e

cd $(dirname $0)/..

./scripts/update_annotations.py scripts/update_annotations_config.json
./scripts/clean_annotations.py src/main/resources/mutation-annotations_SARS2S.json
