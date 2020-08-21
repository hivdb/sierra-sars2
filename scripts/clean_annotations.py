#! /usr/bin/env python3

import re
import sys
import json


def abort(msg, *args, **kwargs):
    print(msg.format(*args, **kwargs), file=sys.stderr)
    exit(1)


def valid_re(pattern):
    try:
        re.compile(pattern)
        return True
    except re.error:
        return False


def clean_annotations(data):
    annotations = []
    dupnames = set()

    for orig_annot in data['annotations']:
        name = orig_annot['name']
        level = orig_annot['level']
        if level == 'amino acid':
            level = 'aminoAcid'
        if name in dupnames:
            abort('Duplicated annot group found: {}', name)
        dupnames.add(name)
        if level not in ('position', 'aminoAcid'):
            abort('Invalid level {!r} for annot group {!r}', level, name)
        hide_citations = bool(orig_annot.get('hideCitations'))
        color_rules = orig_annot.get('colorRules') or []
        if not all(valid_re(r) for r in color_rules):
            abort('Some color rules of annot group {!r} is invalid', name)
        new_annot = {
            'name': name,
            'level': level,
            'hideCitations': hide_citations,
            'colorRules': color_rules
        }
        annotations.append(new_annot)
    data['annotations'] = annotations


def citation_id_sortkey(idx):
    id1, id2 = idx.split('.', 1)
    return (int(id1), int(id2))


def clean_positions(data):
    positions = []

    for posdata in data['positions']:
        position = posdata['position']
        annotations = posdata.get('annotations') or []
        old_aa_annots = posdata.get('aminoAcids') or []
        if old_aa_annots:
            new_aa_annots = {}
            for aadata in old_aa_annots:
                aa = aadata['aminoAcid']
                for annot in aadata['annotations']:
                    name = annot['name']
                    citation_ids = annot['citationIds']
                    new_aa_annots.setdefault(name, {
                        'name': name,
                        'aminoAcids': [],
                        'description': '',
                        'citationIds': set()
                    })
                    new_aa_annots[name]['aminoAcids'].append(aa)
                    new_aa_annots[name]['citationIds'] |= set(citation_ids)
            new_aa_annots = list(new_aa_annots.values())
            for annot in new_aa_annots:
                annot['citationIds'] = sorted(
                    annot['citationIds'],
                    key=citation_id_sortkey)
            annotations.extend(new_aa_annots)
            annotations.sort(key=lambda a: a['name'])
        positions.append({
            'position': position,
            'annotations': annotations
        })
    data['positions'] = sorted(positions, key=lambda p: p['position'])


def main():
    if len(sys.argv) != 2:
        abort('Usage: {} <JSON_PATH>', sys.argv[0])
    json_path = sys.argv[1]
    with open(json_path) as read_fp:
        data = json.load(read_fp)
        clean_annotations(data)
        clean_positions(data)
    data['version'] = '20200821093821'
    with open(json_path, 'w') as write_fp:
        json.dump(data, write_fp, indent=2)


if __name__ == '__main__':
    main()
