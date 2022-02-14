#!/usr/bin/python
import json
from os import path

# Output path for GraalVm Native Image Agent
from pprint import pprint

agent_out_dir = '/Users/jaycarlton/repos/terra-cli/build/agent_out'
# Location for config files in project (under source control). Files in this
# directory will be overwritten with merge results.
native_image_config_dir = '/Users/jaycarlton/repos/terra-cli/src/main/resources/META-INF/native-image'

new_reflect_config = json.load(open(path.join(agent_out_dir, 'reflect-config.json')))
# print(new_reflect_config)

def index_by_name(dict):
   return { dict[u'name']: dict.get(u'methods', '') }

def map_from_file(file):
  entries = json.load(open(file, 'r'))
  return map(index_by_name, entries)

def names_from_reflect_config(config_file):
  entries = json.load(open(config_file, 'r'))
  return { e[u'name'] for e in entries }

source_names = names_from_reflect_config(path.join(agent_out_dir, 'reflect-config.json'))
target_file = path.join(native_image_config_dir, 'reflect-config.json')
target_names = names_from_reflect_config(target_file)

all_names_set = source_names.union(target_names)
all_names_list = sorted(list(all_names_set))

# build JSON structure for all fields, methods, constructors, etc, from each class


map_list = [ { 'name': name,
               'queryAllDeclaredConstructors': True,
               'queryAllPublicConstructors': True,
               'allDeclaredFields': True,
               'queryAllDeclaredMethods': True,
               'queryAllPublicMethods': True,
               'allDeclaredClasses': True,
               'allPublicClasses': True
               } for name in all_names_list ]
json_string = json.dumps(map_list, sort_keys=False, indent=2)
# pprint(json_string)
out_file = open(target_file, 'w')
out_file.write(json_string)
out_file.close()
