#! /bin/sh

set -e

cd "$(dirname $0)/.."

./scripts/import_chiro_prevalence_data.py ../chiro-alignment/local/prevalence src/main/resources/aapcnt