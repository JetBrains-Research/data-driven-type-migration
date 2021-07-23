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


def extract_types_for_readme_table():
    with open(PATH_TO_TYPE_CHANGE_PATTERNS, "r") as file:
        data = json.load(file)
    for pattern in data:
        source_type, target_type = pattern["From"], pattern["To"]
        row = f'| `{source_type}` | `{target_type}` |'
        row = row.replace("$1$", ":[type]").replace("$2$", ":[type2]").replace("3$", ":[type3]")
        row = row.replace("java.lang.", "")
        print(row)


if __name__ == '__main__':
    add_ids()
