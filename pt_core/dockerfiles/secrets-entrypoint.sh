#!/bin/sh -e

# Check that the environment variable has been set correctly
if [ -z "$SECRETS_BUCKET_NAME" ]; then
  echo 'WARNING - missing SECRETS_BUCKET_NAME environment variable: using local variables'
else
    # Override this at the task definition level to temporarily point app to different configuration
    APP_NAME=${TASK_LEVEL_APP_NAME:=pt_core}
    # Load the S3 secrets file contents into the environment variables
    eval $(aws s3 cp s3://${SECRETS_BUCKET_NAME}/${APP_NAME}.txt - | sed 's/^/export /')
    export DD_AGENT_HOST=$(curl --silent http://169.254.169.254/latest/meta-data/local-ipv4)
fi

sh -c "$@"
