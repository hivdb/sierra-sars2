#! /usr/bin/env python3

import sys
import json


def get_max_citation_id(citations):
    return max(cite['citationId'] for cite in citations.values())


def update_from_aapcnt(config):
    with open(config['mutAnnotPath']) as mut_annot_fp:
        mutannot_data = json.load(mut_annot_fp)

    with open(config['dataSourcePath']) as aapcnt_fp:
        aapcnt_data = json.load(aapcnt_fp)

    with open(config['geneDefPath']) as genedef_fp:
        genedef_data = json.load(genedef_fp)
        refseq = None
        for one in genedef_data:
            if one['name'] == config['geneDefGene']:
                refseq = one['refSequence']
                break
        else:
            raise RuntimeError('Can not locate gene')

    for group in mutannot_data['annotations']:
        if group['name'] == config['annotDef']['name']:
            group.update(config['annotDef'])
            break
    else:
        mutannot_data['annotations'].append(
            config['annotDef']
        )

    max_cite_id = get_max_citation_id(mutannot_data['citations'])
    ref_cite_ids = []
    for cite_def in config['citationDefs']:
        for full_cite_id, cite in mutannot_data['citations'].items():
            if cite_def['doi'] == cite['doi'] and \
                    cite_def['section'] == cite['section']:
                cite.update(cite_def)
                ref_cite_ids.append(full_cite_id)
                break
        else:
            max_cite_id += 1
            full_cite_id = '{}.1'.format(max_cite_id)
            mutannot_data['citations'][full_cite_id] = {
                'citationId': max_cite_id,
                'sectionId': 1,
                **cite_def
            }
            ref_cite_ids.append(full_cite_id)

    pos_lookup = {p['position']: p for p in mutannot_data['positions']}
    for aapcnt in aapcnt_data:
        if aapcnt['gene'] != config['dataSourceGene']:
            continue
        pos = aapcnt['position']
        if aapcnt['isUnusual']:
            continue
        ref_aa = refseq[pos - 1]
        aa = aapcnt['aa']
        if ref_aa == aa:
            continue
        posdata = pos_lookup.setdefault(pos, {
            'position': pos,
            'annotations': []
        })
        for annot in posdata['annotations']:
            if annot['name'] == config['annotDef']['name']:
                if aa not in annot['aminoAcids']:
                    annot['aminoAcids'].append(aa)
                annot['citationIds'] = ref_cite_ids
                break
        else:
            posdata['annotations'].append({
                'name': config['annotDef']['name'],
                'aminoAcids': [aa],
                'description': '',
                'citationIds': ref_cite_ids
            })
    mutannot_data['positions'] = sorted(
        pos_lookup.values(), key=lambda p: p['position']
    )
    with open(config['mutAnnotPath'], 'w') as mut_annot_fp:
        json.dump(mutannot_data, mut_annot_fp, indent=2)


def main():
    if len(sys.argv) != 2:
        print('Usage: {} <CONFIG_PATH>'.format(sys.argv[0]), file=sys.stderr)
        exit(1)
    config_path = sys.argv[1]
    with open(config_path) as config_fp:
        all_config = json.load(config_fp)
    for config in all_config:
        if config['dataSourceType'] == 'AAPCNT':
            update_from_aapcnt(config)


if __name__ == '__main__':
    main()
