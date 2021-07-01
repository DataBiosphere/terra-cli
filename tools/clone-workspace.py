#! /usr/bin/python3
# from google.cloud import storage
import json
# from oauth2client.client import GoogleCredentials
import os
import pprint
import shutil
import subprocess
import time
import uuid

# command fragments
TERRA = 'terra'
JSON_FORMAT = '--format=json'
OUT_DIR = '/tmp/cloned_ws7'
pp = pprint.PrettyPrinter(indent=2)


def get_json_stdout(args):
    command = subprocess.run(args, capture_output=True)
    if command.stderr:
        print(command.stderr)
        exit(1)
    return json.loads(command.stdout or "{}")


def get_status():
    return get_json_stdout([TERRA, 'status', JSON_FORMAT])


def get_resources():
    return get_json_stdout([TERRA, 'resources', 'list', JSON_FORMAT])


def clone_resource(resource):
    return {
      'AI_NOTEBOOK': clone_notebook,
      'GCS_BUCKET': clone_bucket,
      'BIG_QUERY_DATASET': clone_bq_dataset
    }[resource['metadata']['resourceType']](resource)


def clone_notebook(resource):
    print(f"Cloning AI notebook {resource['metadata']['name']}...")
    return get_json_stdout(
        [TERRA, 'resources', 'create', 'ai-notebook',
         f"--name=copy_of_{resource['metadata']['name']}",
         f"--description={resource['metadata']['description']}",
         JSON_FORMAT])


def clone_bucket(resource):
    print(f"Cloning bucket {resource['metadata']['name']}...")
    destination_bucket_name = random_name('clone')
    result = get_json_stdout(
        [TERRA, 'resources', 'create', 'gcs-bucket',
         f"--bucket-name={destination_bucket_name}",
         f"--name={resource['metadata']['name']}",
         f"--description={resource['metadata']['description']}",
         JSON_FORMAT])
    args = ['gsutil', 'cp', '-r', f"gs://{resource['resourceAttributes']['gcpGcsBucket']['bucketName']}",
            f'gs://{destination_bucket_name}']
    print(args)
    command = subprocess.run(args, capture_output=True)
    print(command.stdout)
    if command.stderr:
        print(command.stderr)
        exit(1)
    return result


def clone_bq_dataset(resource):
    print(f"Cloning BQ dataset {resource['metadata']['name']}...")
    return get_json_stdout(
        [TERRA, 'resources', 'create', 'bq-dataset',
         f"--dataset-id={'copy_of_' + resource['resourceAttributes']['gcpBqDataset']['datasetId']}",
         f"--name=copy_of_{resource['metadata']['name']}",
         f"--description={resource['metadata']['description']}",
         JSON_FORMAT])


def random_name(prefix):
    return prefix + '-' + uuid.uuid4().hex


start_time = time.time()
status = get_status()
workspace = status['workspace']
source_workspace_id = workspace['id']
source_workspace_name = workspace['displayName']
# print(source_workspace_id)
# pp.pprint(status)

pp.pprint('Gathering resources...')
resources = get_resources()
pp.pprint(resources)

print(f'Cloning workspace {source_workspace_id} into directory {OUT_DIR}...')
if os.path.exists(OUT_DIR):
    shutil.rmtree(OUT_DIR)
os.mkdir(OUT_DIR)
os.chdir(OUT_DIR)
args = ['terra', 'workspace', 'create', JSON_FORMAT,
        '--name="second workspace"',
        "--description=\"Yeah, another workspace." +
        f" Cloned from {source_workspace_id}\""]
create_workspace = subprocess.run(args)  # no output
status2 = get_status()
# pp.pprint(status2)

# Clone each resource in the new workspace
for r in resources:
    result = clone_resource(r)
    pp.pprint(result)

print('Cloned Resources:')
resources = get_resources()
pp.pprint(resources)
end_time = time.time()
elapsed_time = end_time - start_time
print(f'Completed in {elapsed_time} seconds.')
