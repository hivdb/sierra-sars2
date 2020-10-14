#! /bin/sh

set -e

cd "$(dirname $0)/.."

rm -rf /tmp/page-spike-comments.json
curl -sSL 'https://s3-us-west-2.amazonaws.com/cms.hivdb.org/chiro-dev/pages/page-spike-comments.json' -o /tmp/page-spike-comments.json
trap 'rm /tmp/page-spike-comments.json' EXIT

cat > /tmp/cond-comment-config.json <<EOF
{
  "sources": [{
    "cmsPage": "/tmp/page-spike-comments.json",
    "geneName": "S",
    "tableName": "spikeComments"
  }],
  "output": "src/main/resources/conditional-comments.json"
}
EOF
trap 'rm /tmp/cond-comment-config.json' EXIT

python3 ./scripts/import_conditional_comments.py /tmp/cond-comment-config.json
