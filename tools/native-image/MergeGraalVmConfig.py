#!/usr/bin/python3
import json
from _io import open
from builtins import sorted, list
from os import path

# Output path for GraalVm Native Image Agent

AGENT_OUT_DIR = '/Users/jaycarlton/repos/terra-cli/build/agent_out'
# Location for config files in project (under source control). Files in this
# directory will be overwritten with merge results.
NATIVE_IMAGE_CONFIG_DIR = '/Users/jaycarlton/repos/terra-cli/src/main/resources/META-INF/native-image'

new_reflect_config = json.load(
    open(path.join(AGENT_OUT_DIR, 'reflect-config.json')))


def names_from_reflect_config(config_file):
    entries = json.load(open(config_file, 'r'))
    return {e[u'name'] for e in entries}


source_names = names_from_reflect_config(
    path.join(AGENT_OUT_DIR, 'reflect-config.json'))
target_file = path.join(NATIVE_IMAGE_CONFIG_DIR, 'reflect-config.json')
target_names = names_from_reflect_config(target_file)

all_names_set = source_names.union(target_names)
all_names_list = sorted(list(all_names_set))

# build JSON structure for all fields, methods, constructors, etc, from each
# class

map_list = [{'name': name,
             'queryAllDeclaredConstructors': True,
             'queryAllPublicConstructors': True,
             'allDeclaredFields': True,
             'queryAllDeclaredMethods': True,
             'queryAllPublicMethods': True,
             'allDeclaredClasses': True,
             'allPublicClasses': True
             } for name in all_names_list]
# json_string = json.dumps(map_list, sort_keys=False, indent=2)
with open(target_file, 'w') as out_file:
    json.dump(map_list, out_file, indent=2, sort_keys=False)
    # json.dump(json_string, out_file)
#   out_file.write(json_string.decode('utf-8'))
# out_file.close()
