#! /usr/bin/env python3

import sys
import csv
import json


def load_config(config_file):
    config = json.load(config_file)
    citations = {}
    reverse_citations = {}
    cite_autoincur = 1
    annotation_defs = {}
    for key, attrs in config['annotations'].items():
        if key.startswith('#'):
            continue
        annot_name = attrs['name']
        annotation_defs[annot_name] = {
            'name': annot_name,
            'level': attrs['level']
        }
        for citation in attrs['citations']:
            author = citation['author']
            year = citation['year']
            doi = citation['doi']
            section = citation.get('section', '__default__')
            if doi not in reverse_citations:
                reverse_citations[doi] = {
                    'idx': cite_autoincur,
                    'author': author,
                    'year': year,
                    'sections': {}
                }
                cite_autoincur += 1
            r_citation = reverse_citations[doi]
            if section not in r_citation['sections']:
                sections = r_citation['sections']
                if sections:
                    sections[section] = max(sections.values()) + 1
                else:
                    sections[section] = 1
                citations['{}.{}'.format(
                    r_citation['idx'],
                    sections[section]
                )] = {
                    'citationId': r_citation['idx'],
                    'sectionId': sections[section],
                    **citation
                }
    config['citations'] = citations
    config['annotationDefs'] = list(annotation_defs.values())
    config['reverseCitations'] = reverse_citations
    return config


def get_citation_ids(attrs, config):
    all_citations = config['reverseCitations']
    citation_ids = []
    for c in attrs['citations']:
        citation = all_citations[c['doi']]
        section_id = citation['sections'][c['section']]
        idx = (citation['idx'], section_id)
        citation_ids.append(idx)
    return set(citation_ids)


def reform_posdata(posdata):
    posdata['annotations'] = [{
        **annotation,
        'citationIds': [
            '{}.{}'.format(*idx)
            for idx in sorted(annotation['citationIds'])
        ]
    } for annotation in posdata['annotations'].values()]
    posdata['aminoAcids'] = sorted(
        ({
            **aadata,
            'annotations': [{
                **annotation,
                'citationIds': [
                    '{}.{}'.format(*idx)
                    for idx in sorted(annotation['citationIds'])
                ]
            } for annotation in aadata['annotations'].values()]
        } for aadata in posdata['aminoAcids'].values()),
        key=lambda d: d['aminoAcid']
    )
    if not posdata['annotations'] and not posdata['aminoAcids']:
        return
    return posdata


def main():
    if len(sys.argv) != 4:
        print('Usage: {} <CONFIG_PATH> <INPUT_CSV> <OUTPUT_JSON>'
              .format(sys.argv[0]), file=sys.stderr)
        exit(1)
    config, infile, outfile = sys.argv[1:]
    with open(config, 'r', encoding='utf-8-sig') as config, \
            open(infile, 'r', encoding='utf-8-sig') as infile, \
            open(outfile, 'w') as outfile:
        config = load_config(config)
        rows = csv.DictReader(infile)
        all_posdata = []
        for row in rows:
            pos = row['position']
            posdata = {
                'position': pos,
                'annotations': {},
                'aminoAcids': {}
            }
            for key, attrs in config['annotations'].items():
                if key.startswith('#'):
                    continue
                name = attrs['name']
                value = row[key].strip()
                if not value:
                    continue
                level = attrs['level']
                citation_ids = get_citation_ids(attrs, config)
                if level == 'position':
                    annotations = posdata['annotations']
                    annotations.setdefault(name, {
                        'name': name,
                        'value': value,
                        'citationIds': set()
                    })['citationIds'] |= citation_ids
                elif level == 'amino acid':
                    aadata = posdata['aminoAcids']
                    value = (
                        value
                        .replace('ins', 'i')
                        .replace('del', 'd')
                        .replace('unseq', '?')
                        .replace('Unsequenced', '?')
                    )
                    for aa in value:
                        aa = aa.strip()
                        if not aa or aa == '?':
                            continue
                        annotations = (
                            aadata
                            .setdefault(aa, {
                                'aminoAcid': aa,
                                'annotations': {}
                            })['annotations']
                        )
                        annotations.setdefault(name, {
                            'name': name,
                            'citationIds': set()
                        })['citationIds'] |= citation_ids
            posdata = reform_posdata(posdata)
            if posdata:
                all_posdata.append(posdata)
        json.dump({
            'gene': config['gene'],
            'taxonomy': config['taxonomy'],
            'annotations': config['annotationDefs'],
            'citations': config['citations'],
            'positions': sorted(all_posdata, key=lambda d: d['position']),
        }, outfile, indent=2)


if __name__ == '__main__':
    main()
