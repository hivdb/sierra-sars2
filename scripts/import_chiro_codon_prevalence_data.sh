#! /bin/sh

set -e

cd "$(dirname $0)/.."

./scripts/import_chiro_codon_prevalence_data.py ../chiro-alignment/local/prevalence2000 src/main/resources/codonpcnt
