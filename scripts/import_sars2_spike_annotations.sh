#! /bin/bash

set -e

cd $(dirname $0)/..

BOLD=$(tput bold)
NORM=$(tput sgr0)

if ! which ssconvert 1>/dev/null 2>/dev/null ; then
    echo "Abort: unable to find command ${BOLD}ssconvert${NORM} from package ${BOLD}gnumeric${NORM}." 1>&2
    exit 1
fi

XLSX_PATH="$HOME/dropbox/Coronavirus/Analyses/SpikeAnnotationRS.xlsx"
CSV_PATH=$(mktemp)

trap "rm $CSV_PATH" EXIT

ssconvert $XLSX_PATH $CSV_PATH --export-type="Gnumeric_stf:stf_csv"
scripts/import_annotations.py scripts/sars2_spike_annotations.json $CSV_PATH src/main/resources/mutation-annotations_SARS2S.json
