#! /usr/bin/env python3

import sys
import json


def get_max_citation_id(citations):
    if not citations:
        return 1
    return max(cite['citationId'] for cite in citations.values())


def merge_annotation_group(config, mutannot_data):
    mutannot_data['annotations'] = mutannot_data.get('annotations', [])
    for group in mutannot_data['annotations']:
        if group['name'] == config['annotDef']['name']:
            group.update(config['annotDef'])
            break
    else:
        mutannot_data['annotations'].append(
            config['annotDef']
        )


def merge_citations(config, mutannot_data):
    max_cite_id = get_max_citation_id(mutannot_data.get('citations'))
    ref_cite_ids = []
    for cite_def in config['citationDefs']:
        for full_cite_id, cite in mutannot_data.get('citations', {}).items():
            if cite_def['doi'] == cite['doi'] and \
                    cite_def['section'] == cite['section']:
                cite.update(cite_def)
                ref_cite_ids.append(full_cite_id)
                break
        else:
            max_cite_id += 1
            full_cite_id = '{}.1'.format(max_cite_id)
            mutannot_data.get('citations', {})[full_cite_id] = {
                'citationId': max_cite_id,
                'sectionId': 1,
                **cite_def
            }
            ref_cite_ids.append(full_cite_id)
    return ref_cite_ids


def merge_annotations(positions, annot_lookup, annot_name, ref_cite_ids):
    for posdata in positions:
        pos = posdata['position']
        annotdata = annot_lookup.pop(pos, None)
        if annotdata:
            target_annot = None
            for annot in posdata['annotations']:
                if annot['name'] == annot_name:
                    target_annot = annot
                    break
            else:
                target_annot = {
                    'name': annot_name
                }
                posdata['annotations'].append(target_annot)
            target_annot['citationIds'] = ref_cite_ids
            target_annot.update(annotdata)
        else:
            # delete annotation
            posdata['annotations'] = [
                annot for annot in posdata['annotations']
                if annot['name'] != annot_name
            ]
    if annot_lookup:
        for pos, annotdata in annot_lookup.items():
            # create position
            positions.append({
                'position': pos,
                'annotations': [{
                    'name': annot_name,
                    'citationIds': ref_cite_ids,
                    **annotdata
                }]
            })
    return sorted(positions, key=lambda p: p['position'])


def update_aa_annots(config):
    with open(config['mutAnnotPath']) as mut_annot_fp:
        mutannot_data = json.load(mut_annot_fp)

    with open(config['dataSourcePath']) as aapcnt_fp:
        annots = json.load(aapcnt_fp)

    merge_annotation_group(config, mutannot_data)
    ref_cite_ids = merge_citations(config, mutannot_data)

    annot_name = config['annotDef']['name']
    pos_lookup = {}
    for annot in annots:
        pos = annot['position']
        aas = set()
        for col_name in config['dataSourceAAColumns']:
            aas |= set(annot[col_name])
        if not aas:
            continue
        aas = sorted(aas)
        pos_lookup[pos] = {
            'aminoAcids': aas
        }
    mutannot_data['positions'] = merge_annotations(
        mutannot_data['positions'], pos_lookup, annot_name, ref_cite_ids
    )
    with open(config['mutAnnotPath'], 'w') as mut_annot_fp:
        json.dump(mutannot_data, mut_annot_fp, indent=2)


def remove_annots(config):
    with open(config['mutAnnotPath']) as mut_annot_fp:
        mutannot_data = json.load(mut_annot_fp)
    mutannot_data['annotations'] = [
        annot for annot in mutannot_data['annotations']
        if annot['name'] != config['annotName']
    ]
    for posdata in mutannot_data['positions']:
        posdata['annotations'] = [
            annot for annot in posdata['annotations']
            if annot['name'] != config['annotName']
        ]
    with open(config['mutAnnotPath'], 'w') as mut_annot_fp:
        json.dump(mutannot_data, mut_annot_fp, indent=2)


def update_pos_annots(config):
    with open(config['mutAnnotPath']) as mut_annot_fp:
        mutannot_data = json.load(mut_annot_fp)
    merge_annotation_group(config, mutannot_data)
    ref_cite_ids = merge_citations(config, mutannot_data)
    # pos_lookup = {p['position']: p for p in mutannot_data['positions']}
    annot_name = config['annotDef']['name']
    pos_lookup = {}
    for annot in config['annotations']:
        value = annot['value']
        description = annot.get('description', '')
        for pos_range in annot['positions']:
            if isinstance(pos_range, int):
                pos_range = [pos_range, pos_range]
            for pos in range(pos_range[0], pos_range[1] + 1):
                pos_lookup[pos] = {
                    'value': value,
                    'description': description
                }
    mutannot_data['positions'] = merge_annotations(
        mutannot_data.get('positions', []), pos_lookup, annot_name, ref_cite_ids
    )
    with open(config['mutAnnotPath'], 'w') as mut_annot_fp:
        json.dump(mutannot_data, mut_annot_fp, indent=2)


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

    merge_annotation_group(config, mutannot_data)
    ref_cite_ids = merge_citations(config, mutannot_data)

    pos_lookup = {p['position']: p for p in mutannot_data['positions']}
    for aapcnt in aapcnt_data:
        if aapcnt['gene'] != config['dataSourceGene']:
            continue
        pos = aapcnt['position']
        if aapcnt['percent'] < config['dataSourceCutoff']:
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
        elif config['dataSourceType'] == 'POS_ANNOTS':
            update_pos_annots(config)
        elif config['dataSourceType'] == 'AA_ANNOTS':
            update_aa_annots(config)
        elif config['dataSourceType'] == 'DEL_ANNOTS':
            remove_annots(config)


if __name__ == '__main__':
    main()
