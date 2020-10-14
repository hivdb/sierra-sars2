import sys
import json

DEFAULT_STRAIN = 'SARS2'
DEFAULT_DRUG_CLASS = None


def main():
    if len(sys.argv) != 2:
        print('Usage: {} <CONFIG_JSON>', file=sys.stderr)
        exit(1)
    with open(sys.argv[1], encoding='utf-8-sig') as fp:
        config = json.load(fp)

    results = []
    for source in config['sources']:
        gene_name = source['geneName']
        in_json = source['cmsPage']
        table_name = source['tableName']
        with open(in_json) as fp:
            data = json.load(fp)
            data = data['tables'][table_name]['data']
        for row in data:
            triggered_aas = row['triggeredAAs']
            # stop codon is not supported
            triggered_aas = triggered_aas.replace('*', '')
            if not triggered_aas:
                continue
            pos = row['position']
            comment = row['comment']
            strain = row.get('strain', DEFAULT_STRAIN)
            drug_class = row.get('drugClass', DEFAULT_DRUG_CLASS)
            results.append({
                'strain': strain,
                'commentName': '{}{}{}'.format(gene_name, pos, triggered_aas),
                'drugClass': drug_class,
                'conditionType': 'MUTATION',
                'conditionValue': {
                    'gene': gene_name,
                    'pos': pos,
                    'aas': triggered_aas
                },
                'comment': comment
            })

    out_json = config['output']
    with open(out_json, 'w') as fp:
        json.dump(results, fp, indent=2)


if __name__ == '__main__':
    main()
