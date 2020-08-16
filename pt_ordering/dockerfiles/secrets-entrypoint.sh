#!/bin/bash -e

# Check that the environment variable has been set correctly
if [ -z "$SECRETS_BUCKET_NAME" ]; then
  echo 'WARNING - missing SECRETS_BUCKET_NAME environment variable: using local variables'
else
    # Load the S3 secrets file contents into the environment variables
    eval $(aws s3 cp s3://${SECRETS_BUCKET_NAME}/pt_ordering.txt - | sed 's/^/export /')
    export NEW_RELIC_APP_NAME="$NEW_RELIC_AGGREGATED_APP_NAME"
fi

sh -c "$@"
