#! /usr/bin/env python3
import os
import sys
import json


def sort_poscodons(poscodons):
    genes = ['RdRP', 'S']
    return sorted(poscodons, key=lambda r: (
        genes.index(r['gene']),
        r['position'],
        r['codon']
    ))


DIST_FILE_CONFIGS = [
    {
        'filename': 'rx-all_taxon-SARS2.json',
        'taxonomy': 'SARS2',
        'unusual_cutoff': 0.01,
        'genes': {'S', 'RdRP'},
    },
    {
        'filename': 'rx-all_taxon-SARS.json',
        'taxonomy': 'SARS',
        'unusual_cutoff': 0.01,
        'genes': {'S', 'RdRP'},
    },
    {
        'filename': 'rx-all_taxon-SARSr.json',
        'taxonomy': 'SARSr+NC_004718+NC_045512',
        'unusual_cutoff': 0.02,
        'genes': {'S', 'RdRP'},
    },
]
JSON_PREFIX = 'codon.prevalence.with-mixtures.verbose.'


def export_compat_data(data, output_config):
    genes = output_config['genes']
    taxonomy = output_config['taxonomy']
    rows = []
    for gene, genedata in data.items():
        if gene not in genes:
            continue
        for posdata in genedata:
            for taxondata in posdata['taxon_detail']:
                if taxondata['taxonomy'] != taxonomy:
                    continue
                for cddata in taxondata['codon_variants']:
                    rows.append({
                        'gene': gene,
                        'position': posdata['position'],
                        'codon': cddata['seq_codon'],
                        'percent': cddata['prevalence'],
                        'count': cddata['count'],
                        'total': taxondata['total']
                    })
    return rows


def main():
    if len(sys.argv) != 3:
        print('Usage: {} <INPUT_DIRECTORY> <OUTPUT_DIRECTORY>'
              .format(sys.argv[0]), file=sys.stderr)
        exit(1)
    indir, outdir = sys.argv[1:]
    os.makedirs(outdir, exist_ok=True)
    all_data = {}
    for root, _, files in os.walk(indir):
        for jsonfile in files:
            if not jsonfile.endswith('.json'):
                continue
            if not jsonfile.startswith(JSON_PREFIX):
                continue
            gene = jsonfile[len(JSON_PREFIX):-5]
            with open(
                os.path.join(root, jsonfile),
                'r',
                encoding='utf-8-sig'
            ) as fp:
                all_data[gene] = json.load(fp)
    all_outputs = []
    for config in DIST_FILE_CONFIGS:
        all_outputs.append(export_compat_data(all_data, config))
    for config, output in zip(DIST_FILE_CONFIGS, all_outputs):
        postprocess = config.get('postprocess')
        if postprocess:
            output = postprocess(output, all_outputs)
        output = sort_poscodons(output)
        with open(
            os.path.join(outdir, config['filename']),
            'w'
        ) as fp:
            json.dump(output, fp, indent=2)


if __name__ == '__main__':
    main()
