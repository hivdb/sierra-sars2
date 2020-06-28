#! /usr/bin/env python3
import os
import sys
import json


def postprocess_sars2(data, all_data):
    usual_aas = set()
    for rows in all_data:
        for row in rows:
            if not row['isUnusual']:
                usual_aas.add((row['gene'], row['position'], row['aa']))
    totals = {
        (r['gene'], r['position']): r['total']
        for r in data
    }
    posaamap = {
        (r['gene'], r['position'], r['aa']): r
        for r in data
    }
    for usual_aa in usual_aas:
        gene, pos, aa = usual_aa
        if usual_aa not in posaamap:
            posaamap[usual_aa] = {
                'gene': gene,
                'position': pos,
                'aa': aa,
                'percent': 0,
                'count': 0,
                'total': totals[(gene, pos)],
                'reason': 'IN_SIMILAR_VIRUS',
                'isUnusual': False
            }
        elif posaamap[usual_aa]['isUnusual']:
            posaamap[usual_aa].update({
                'reason': 'IN_SIMILAR_VIRUS',
                'isUnusual': False
            })
    return sorted(
        posaamap.values(),
        key=lambda r: (r['gene'], r['position'], r['aa'])
    )


DIST_FILE_CONFIGS = [
    {
        'filename': 'rx-all_taxon-SARS2.json',
        'taxonomy': 'SARS2',
        'unusual_cutoff': 0.01,
        'genes': {'S', 'RdRP'},
        'postprocess': postprocess_sars2
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
JSON_PREFIX = 'prevalence.with-mixtures.'


def export_compat_data(data, output_config):
    filename = output_config['filename']
    genes = output_config['genes']
    taxonomy = output_config['taxonomy']
    cutoff = output_config['unusual_cutoff']
    rows = []
    for gene, genedata in data.items():
        if gene not in genes:
            continue
        for posdata in genedata:
            for taxondata in posdata['taxon_detail']:
                if taxondata['taxonomy'] != taxonomy:
                    continue
                for aadata in taxondata['aa_variants']:
                    rows.append({
                        'gene': gene,
                        'position': posdata['position'],
                        'aa': aadata['seq_aa'],
                        'percent': aadata['prevalence'],
                        'count': aadata['count'],
                        'total': taxondata['total'],
                        'reason': 'PCNT',
                        'isUnusual': aadata['prevalence'] < cutoff
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
        with open(
            os.path.join(outdir, config['filename']),
            'w'
        ) as fp:
            json.dump(output, fp, indent=2)


if __name__ == '__main__':
    main()
