import json

PATH_TO_TYPE_CHANGE_PATTERNS = "../plugin/src/main/resources/rules.json"
PATH_TO_TYPE_CHANGE_PATTERNS_WITH_IDS = "../plugin/src/main/resources/rules_with_ids.json"
PATH_TO_UNIQUE_TYPES = "../plugin/src/main/resources/enum.json"


def extract_unique_types():
    with open(PATH_TO_TYPE_CHANGE_PATTERNS, "r") as file:
        data = json.load(file)
    unique_types = set()
    for pattern in data:
        unique_types.add(pattern['From'])
        unique_types.add(pattern['To'])
    result = {"type_patterns": list(unique_types)}
    with open(PATH_TO_UNIQUE_TYPES, "w") as file:
        json.dump(result, file)


def add_ids():
    with open(PATH_TO_TYPE_CHANGE_PATTERNS, "r") as file:
        data = json.load(file)
    id = 1
    for pattern in data:
        pattern['ID'] = id
        id += 1
    with open(PATH_TO_TYPE_CHANGE_PATTERNS_WITH_IDS, "w") as file:
        json.dump(data, file)


if __name__ == '__main__':
    add_ids()
